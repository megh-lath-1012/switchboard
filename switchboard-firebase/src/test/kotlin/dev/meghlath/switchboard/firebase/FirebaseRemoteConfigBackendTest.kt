package dev.meghlath.switchboard.firebase

import app.cash.turbine.test
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dev.meghlath.switchboard.core.LogLevel
import dev.meghlath.switchboard.core.Switchboard
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FirebaseRemoteConfigBackendTest {

    private val remoteConfig = mockk<FirebaseRemoteConfig>()
    private lateinit var backend: FirebaseRemoteConfigBackend
    private var lastLogLevel: LogLevel? = null
    private var lastLogMessage: String? = null

    @BeforeEach
    public fun setUp() {
        backend = FirebaseRemoteConfigBackend(remoteConfig)
        lastLogLevel = null
        lastLogMessage = null
        Switchboard.setLogHandler { level, message, _ ->
            lastLogLevel = level
            lastLogMessage = message
        }
    }

    @Test
    public fun testBooleanCoercionSuccess(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "true"
        backend.getBoolean("key") shouldBe true
    }

    @Test
    public fun testBooleanCoercionFailure(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "banana"
        backend.getBoolean("key") shouldBe null
        lastLogLevel shouldBe LogLevel.WARN
        lastLogMessage shouldBe "FirebaseRemoteConfigBackend: Failed to coerce value 'banana' for key 'key'"
    }

    @Test
    public fun testIntCoercionSuccess(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "5000"
        backend.getInt("key") shouldBe 5000
    }

    @Test
    public fun testIntCoercionFailure(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "3.14"
        backend.getInt("key") shouldBe null
        lastLogLevel shouldBe LogLevel.WARN
    }

    @Test
    public fun testLongCoercionSuccess(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "1234567890"
        backend.getLong("key") shouldBe 1234567890L
    }

    @Test
    public fun testFloatCoercionSuccess(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "1.5"
        backend.getFloat("key") shouldBe 1.5f
    }

    @Test
    public fun testDoubleCoercionSuccess(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "3.14159"
        backend.getDouble("key") shouldBe 3.14159
    }

    @Test
    public fun testStringEmpty(): Unit = runTest {
        every { remoteConfig.getString("key") } returns ""
        backend.getString("key") shouldBe null
    }

    @Test
    public fun testStringNonEmpty(): Unit = runTest {
        every { remoteConfig.getString("key") } returns "hello"
        backend.getString("key") shouldBe "hello"
    }

    @Test
    public fun testChangesFlowEmitsOnUpdate(): Unit = runTest {
        val registration = mockk<ConfigUpdateListenerRegistration>(relaxed = true)
        val listenerSlot = slot<ConfigUpdateListener>()
        every { remoteConfig.addOnConfigUpdateListener(capture(listenerSlot)) } returns registration

        backend.changes().test {
            listenerSlot.captured.onUpdate(mockk(relaxed = true))
            awaitItem() shouldBe Unit
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    public fun testChangesFlowCancelsListener(): Unit = runTest {
        val registration = mockk<ConfigUpdateListenerRegistration>(relaxed = true)
        every { remoteConfig.addOnConfigUpdateListener(any()) } returns registration

        backend.changes().test {
            cancelAndIgnoreRemainingEvents()
        }
        
        verify { registration.remove() }
    }
}
