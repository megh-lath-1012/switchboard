package dev.meghlath.switchboard.okhttp

import dev.meghlath.switchboard.annotations.InternalSwitchboardApi
import dev.meghlath.switchboard.core.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.*
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.emptyFlow

@OptIn(InternalSwitchboardApi::class)
internal class SwitchboardInterceptorTest {

    private val chain = mockk<Interceptor.Chain>()
    private val request = Request.Builder().url("https://example.com").method("GET", null).build()
    private val response = mockk<Response>()
    
    private var loggedMessage: String? = null

    @BeforeEach
    public fun setUp() {
        resetSwitchboard()
        loggedMessage = null
        Switchboard.setLogHandler { _, message, _ ->
            loggedMessage = message
        }
        
        every { chain.request() } returns request
        every { chain.proceed(any()) } returns response
    }

    @AfterEach
    public fun tearDown() {
        resetSwitchboard()
        unmockkAll()
    }

    private fun resetSwitchboard() {
        // Kotlin internal methods are mangled in bytecode. 
        // We use reflection to find the correct method name for cleanup.
        val resetMethod = Switchboard::class.java.methods.find { it.name.startsWith("resetForTest") }
        resetMethod?.isAccessible = true
        resetMethod?.invoke(Switchboard)
    }

    private fun initSwitchboard(
        debug: Boolean = true, 
        flags: List<RegisteredFlag> = emptyList(),
        overrides: Map<String, String> = emptyMap(),
        backend: Backend = NoOpBackend
    ) {
        val storage = mockk<OverrideStorage>()
        coEvery { storage.read() } returns overrides
        every { storage.changes() } returns emptyFlow()
        
        val context = mockk<SwitchboardContext>()
        every { context.overrideStorage() } returns storage
        
        val registry = object : SwitchboardRegistry {
            override val flags = flags
        }
        
        Switchboard.init(context, registry, backend, debugEnabled = debug)
        runBlocking { Switchboard.awaitIdle() }
    }

    @Test
    public fun testAllLevelLogsAllFlags() {
        val flags = listOf(
            RegisteredFlag("flag1", FlagType.BOOLEAN, "true", "desc", "cat"),
            RegisteredFlag("flag2", FlagType.INT, "10", "desc", "cat")
        )
        initSwitchboard(flags = flags)
        
        val interceptor = SwitchboardInterceptor(SwitchboardLogLevel.ALL)
        interceptor.intercept(chain)
        
        loggedMessage shouldContain "Switchboard ▶ [GET] https://example.com/"
        loggedMessage shouldContain "flag1: true [DEFAULT]"
        loggedMessage shouldContain "flag2: 10 [DEFAULT]"
    }

    @Test
    public fun testOverridesOnlyLogsOnlyOverriddenFlags() {
        val flags = listOf(
            RegisteredFlag("flag1", FlagType.BOOLEAN, "true", "desc", "cat"),
            RegisteredFlag("flag2", FlagType.INT, "10", "desc", "cat")
        )
        initSwitchboard(flags = flags, overrides = mapOf("flag1" to "false"))
        
        val interceptor = SwitchboardInterceptor(SwitchboardLogLevel.OVERRIDES_ONLY)
        interceptor.intercept(chain)
        
        loggedMessage shouldContain "flag1: false [OVERRIDE ⚠️]"
        loggedMessage shouldNotContain "flag2"
    }

    @Test
    public fun testNoFlagsRegistered() {
        initSwitchboard(flags = emptyList())
        
        val interceptor = SwitchboardInterceptor(SwitchboardLogLevel.ALL)
        interceptor.intercept(chain)
        
        loggedMessage shouldBe null
        verify { chain.proceed(any()) }
    }

    @Test
    public fun testRequestStillProceedsOnException() {
        mockkObject(SwitchboardDebugAccessor)
        every { SwitchboardDebugAccessor.isDebugEnabled() } returns true
        every { SwitchboardDebugAccessor.getRegistry() } throws RuntimeException("Explosion")
        
        val interceptor = SwitchboardInterceptor(SwitchboardLogLevel.ALL)
        val res = interceptor.intercept(chain)
        
        res shouldBe response
        verify { chain.proceed(any()) }
    }

    @Test
    public fun testOverrideFlagHasWarningGlyph() {
        val flags = listOf(RegisteredFlag("flag1", FlagType.BOOLEAN, "true", "desc", "cat"))
        initSwitchboard(flags = flags, overrides = mapOf("flag1" to "false"))
        
        val interceptor = SwitchboardInterceptor(SwitchboardLogLevel.OVERRIDES_ONLY)
        interceptor.intercept(chain)
        
        loggedMessage shouldContain "⚠️"
    }

    @Test
    public fun testLogOutputFormat() {
        val flags = listOf(RegisteredFlag("flag1", FlagType.BOOLEAN, "true", "desc", "cat"))
        initSwitchboard(flags = flags)
        
        val interceptor = SwitchboardInterceptor(SwitchboardLogLevel.ALL)
        interceptor.intercept(chain)
        
        val lines = loggedMessage!!.split("\n")
        lines[0] shouldBe "Switchboard ▶ [GET] https://example.com/"
        lines[1] shouldBe "  ↳ flag1: true [DEFAULT]"
    }
}
