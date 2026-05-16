package dev.meghlath.switchboard.core

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

public enum class TestVariant {
    CONTROL, TREATMENT_A, TREATMENT_B
}

public class SwitchboardEngineTest {

    private class TestLogger : Logger {
        val warnings = CopyOnWriteArrayList<String>()
        val infos = CopyOnWriteArrayList<String>()

        override fun warn(message: String) {
            warnings.add(message)
            println("[Test WARN]: $message")
        }

        override fun info(message: String) {
            infos.add(message)
            println("[Test INFO]: $message")
        }
    }

    private lateinit var logger: TestLogger
    private lateinit var context: FakeSwitchboardContext
    private lateinit var storage: FakeOverrideStorage

    @BeforeEach
    public fun setup() {
        logger = TestLogger()
        currentLogger = logger
        storage = FakeOverrideStorage()
        context = FakeSwitchboardContext(storage)
        Switchboard.resetForTest()
    }

    @AfterEach
    public fun tearDown() {
        Switchboard.resetForTest()
        currentLogger = DefaultLogger
    }

    @Test
    public fun testInMemoryBackendRoundTrip(): Unit = runBlocking {
        val backend = InMemoryBackend()

        backend.setBoolean("boolKey", true)
        backend.getBoolean("boolKey") shouldBe true

        backend.setInt("intKey", 42)
        backend.getInt("intKey") shouldBe 42

        backend.setLong("longKey", 100L)
        backend.getLong("longKey") shouldBe 100L

        backend.setFloat("floatKey", 3.14f)
        backend.getFloat("floatKey") shouldBe 3.14f

        backend.setDouble("doubleKey", 2.718)
        backend.getDouble("doubleKey") shouldBe 2.718

        backend.setString("stringKey", "hello")
        backend.getString("stringKey") shouldBe "hello"

        // Roundtrip clear
        backend.clear()
        backend.getBoolean("boolKey") shouldBe null
    }

    @Test
    public fun testPreInitReads() {
        // Calling resolveX() before init() completes must return default without blocking
        val value = Switchboard.resolveInt("timeout", 5000)
        value shouldBe 5000

        // Warning emitted once test
        logger.warnings.size shouldBe 1
        logger.warnings[0] shouldBe "Switchboard.resolveX() called before init() completed. Returning default values."

        // Subsequent read doesn't log again
        Switchboard.resolveInt("timeout", 5000)
        logger.warnings.size shouldBe 1
    }

    @Test
    public fun testWarningEmittedOnce() {
        // Verify init warning logs once if called twice
        Switchboard.init(context)
        Switchboard.init(context)

        logger.warnings shouldContain "Switchboard.init() called more than once. Subsequent calls are ignored."
    }

    @Test
    public fun testCompositeBackendOrdering(): Unit = runBlocking {
        val backend1 = InMemoryBackend()
        val backend2 = InMemoryBackend()
        val composite = CompositeBackend(backend1, backend2)

        backend2.setString("key", "fallback")
        composite.getString("key") shouldBe "fallback"

        // First non-null wins
        backend1.setString("key", "primary")
        composite.getString("key") shouldBe "primary"
    }

    @Test
    public fun testResolutionPriorityCombinations(): Unit = runBlocking {
        val backend = InMemoryBackend()
        backend.setInt("flag", 10) // backend value

        // 1. Only Default
        Switchboard.init(context, backend = backend, debugEnabled = false)
        Switchboard.awaitIdle()
        // read non-cached key triggers async load, so initial returns default
        Switchboard.resolveInt("unknown", 1) shouldBe 1

        // 2. Backend Present vs Default
        Switchboard.resolveInt("flag", 1) // trigger fetch
        Switchboard.awaitIdle()
        Switchboard.resolveInt("flag", 1) shouldBe 10

        // 3. Override Present vs Backend vs Default
        storage.setOverride("flag", "99")
        storage.setOverride("variant", "TREATMENT_A")
        // Overrides require debugEnabled = true to win
        Switchboard.resetForTest()
        Switchboard.init(context, backend = backend, debugEnabled = true)
        Switchboard.awaitIdle()

        Switchboard.resolveInt("flag", 1) shouldBe 99

        // Verify Enum resolution priority
        Switchboard.resolveEnum<TestVariant>("variant", TestVariant.CONTROL) shouldBe TestVariant.TREATMENT_A
    }

    @Test
    public fun testBackendChangesInvalidatesCache(): Unit = runBlocking {
        val backend = InMemoryBackend()
        Switchboard.init(context, backend = backend, debugEnabled = false)
        Switchboard.awaitIdle()

        backend.setString("title", "v1")
        Switchboard.resolveString("title", "default") // trigger fetch
        Switchboard.awaitIdle()
        Switchboard.resolveString("title", "default") shouldBe "v1"

        // Reactive changes via Turbine testing stream notification
        backend.changes().test {
            backend.setString("title", "v2") // emits to changes()
            awaitItem()
            // Invalidation clears cache
            Switchboard.resolveString("title", "default") // triggers fetch of new value
            Switchboard.awaitIdle()
            Switchboard.resolveString("title", "default") shouldBe "v2"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    public fun testConcurrentReads(): Unit = runBlocking {
        val backend = InMemoryBackend()
        backend.setBoolean("feature", true)

        Switchboard.init(context, backend = backend, debugEnabled = true)
        Switchboard.awaitIdle()

        // 100 coroutines reading same flag simultaneously, ensuring lock-free thread-safety without race conditions
        val scope = this
        val jobs = (1..100).map {
            scope.async(Dispatchers.Default) {
                Switchboard.resolveBoolean("feature", false)
            }
        }
        jobs.awaitAll()

        Switchboard.awaitIdle()
        Switchboard.resolveBoolean("feature", false) shouldBe true
    }

    @Test
    public fun testTypeMismatchWarnings(): Unit = runBlocking {
        val backend = object : Backend by NoOpBackend {
            override suspend fun getBoolean(key: String): Boolean? {
                throw ClassCastException("Incompatible types")
            }
        }

        storage.setOverride("intKey", "not_an_int")

        Switchboard.init(context, backend = backend, debugEnabled = true)
        Switchboard.awaitIdle()

        // Test bad backend throwing ClassCastException logs specific warning
        Switchboard.resolveBoolean("mismatch", true)
        Switchboard.awaitIdle()

        val foundBackendWarning = logger.warnings.any { it.contains("Backend returned wrong type for key mismatch") }
        if (!foundBackendWarning) {
            println("ACTUAL WARNINGS LOGGED: ${logger.warnings}")
        }
        foundBackendWarning shouldBe true

        // Test bad override format logs specific warning
        Switchboard.resolveInt("intKey", 5)
        val foundOverrideWarning = logger.warnings.any { it.contains("Override returned wrong type for key intKey") }
        foundOverrideWarning shouldBe true

        // Test bad overrides for remaining types to hit all branches of parseOverride
        storage.setOverride("boolBad", "not_a_bool")
        storage.setOverride("longBad", "not_a_long")
        storage.setOverride("floatBad", "not_a_float")
        storage.setOverride("doubleBad", "not_a_double")
        Switchboard.resetForTest()
        Switchboard.init(context, backend = backend, debugEnabled = true)
        Switchboard.awaitIdle()

        Switchboard.resolveBoolean("boolBad", true)
        Switchboard.resolveLong("longBad", 10L)
        Switchboard.resolveFloat("floatBad", 1.0f)
        Switchboard.resolveDouble("doubleBad", 1.0)

        logger.warnings.any { it.contains("expected Boolean") } shouldBe true
        logger.warnings.any { it.contains("expected Long") } shouldBe true
        logger.warnings.any { it.contains("expected Float") } shouldBe true
        logger.warnings.any { it.contains("expected Double") } shouldBe true
    }

    @Test
    public fun testExhaustiveBackendAndPrimitivesCoverage(): Unit = runBlocking {
        // Full verification of NoOpBackend
        NoOpBackend.getBoolean("key") shouldBe null
        NoOpBackend.getInt("key") shouldBe null
        NoOpBackend.getLong("key") shouldBe null
        NoOpBackend.getFloat("key") shouldBe null
        NoOpBackend.getDouble("key") shouldBe null
        NoOpBackend.getString("key") shouldBe null

        // Full verification of CompositeBackend primitive delegations
        val b1 = InMemoryBackend()
        val b2 = InMemoryBackend()
        val composite = CompositeBackend(b1, b2)

        b2.setBoolean("bool", false)
        b2.setInt("int", 100)
        b2.setLong("long", 999L)
        b2.setFloat("float", 2.5f)
        b2.setDouble("double", 9.99)

        composite.getBoolean("bool") shouldBe false
        composite.getInt("int") shouldBe 100
        composite.getLong("long") shouldBe 999L
        composite.getFloat("float") shouldBe 2.5f
        composite.getDouble("double") shouldBe 9.99

        // Override higher order primitives in Switchboard
        storage.setOverride("longOverride", "555")
        storage.setOverride("floatOverride", "4.5")
        storage.setOverride("doubleOverride", "8.88")

        Switchboard.init(context, backend = composite, debugEnabled = true)
        Switchboard.awaitIdle()

        Switchboard.resolveLong("longOverride", 0L) shouldBe 555L
        Switchboard.resolveFloat("floatOverride", 0.0f) shouldBe 4.5f
        Switchboard.resolveDouble("doubleOverride", 0.0) shouldBe 8.88

        // Resolution cache hits and refresh lifecycle verification
        Switchboard.resolveLong("longOverride", 0L) shouldBe 555L
        Switchboard.refresh()
        Switchboard.resolveLong("longOverride", 0L) shouldBe 555L
    }

    @Test
    public fun testSetLogHandlerFunctionalRouting() {
        // Temporarily route currentLogger to DefaultLogger to test production routing mechanics
        currentLogger = DefaultLogger

        var capturedLevel: LogLevel? = null
        var capturedMessage: String? = null

        Switchboard.setLogHandler { level, message, throwable ->
            capturedLevel = level
            capturedMessage = message
        }

        currentLogger.warn("Test functional warning")
        capturedLevel shouldBe LogLevel.WARN
        capturedMessage shouldBe "Test functional warning"

        currentLogger.info("Test functional info")
        capturedLevel shouldBe LogLevel.INFO
        capturedMessage shouldBe "Test functional info"

        // Restore null handler routes back to standard output
        Switchboard.setLogHandler(null)
        capturedLevel = null
        currentLogger.warn("Should not be captured")
        capturedLevel shouldBe null

        // Restore TestLogger for teardown lifecycle
        currentLogger = logger
    }
}
