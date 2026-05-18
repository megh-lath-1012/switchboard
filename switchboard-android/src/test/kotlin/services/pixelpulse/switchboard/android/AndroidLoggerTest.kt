package services.pixelpulse.switchboard.android

import android.util.Log
import services.pixelpulse.switchboard.core.LogLevel
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

public class AndroidLoggerTest {

    @BeforeEach
    public fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.i(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @AfterEach
    public fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    public fun testInvokeWithoutThrowableRoutesToStandardLogOverloads() {
        AndroidLogger.invoke(LogLevel.DEBUG, "debug msg", null)
        verify(exactly = 1) { Log.d("Switchboard", "debug msg") }

        AndroidLogger.invoke(LogLevel.INFO, "info msg", null)
        verify(exactly = 1) { Log.i("Switchboard", "info msg") }

        AndroidLogger.invoke(LogLevel.WARN, "warn msg", null)
        verify(exactly = 1) { Log.w("Switchboard", "warn msg") }

        AndroidLogger.invoke(LogLevel.ERROR, "error msg", null)
        verify(exactly = 1) { Log.e("Switchboard", "error msg") }
    }

    @Test
    public fun testInvokeWithThrowableRoutesToThrowableLogOverloads() {
        val ex = RuntimeException("test exception")

        AndroidLogger.invoke(LogLevel.DEBUG, "debug err", ex)
        verify(exactly = 1) { Log.d("Switchboard", "debug err", ex) }

        AndroidLogger.invoke(LogLevel.INFO, "info err", ex)
        verify(exactly = 1) { Log.i("Switchboard", "info err", ex) }

        AndroidLogger.invoke(LogLevel.WARN, "warn err", ex)
        verify(exactly = 1) { Log.w("Switchboard", "warn err", ex) }

        AndroidLogger.invoke(LogLevel.ERROR, "error err", ex)
        verify(exactly = 1) { Log.e("Switchboard", "error err", ex) }
    }
}
