package services.pixelpulse.switchboard.android

import android.content.Context
import android.util.Log
import services.pixelpulse.switchboard.annotations.SwitchboardRegistryOmitted
import services.pixelpulse.switchboard.core.Switchboard
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

@OptIn(SwitchboardRegistryOmitted::class)
public class SwitchboardAndroidIntegrationTest {

    private lateinit var context: Context
    private lateinit var storage: DataStoreOverrideStorage

    @BeforeEach
    public fun setup() {
        // Intercept native android Log calls to prevent JVM stub runtime exceptions
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        context = mockk<Context>()
        val tempDir = Files.createTempDirectory("integration_datastore").toFile()
        java.io.File(tempDir, "datastore").mkdirs()
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempDir

        // Obtain reference to the same storage delegate instance via public constructor mapping
        storage = DataStoreOverrideStorage(context)
    }

    @AfterEach
    public fun tearDown() {
        unmockkStatic(Log::class)
        Switchboard.setLogHandler(null)
        val method = Switchboard::class.java.declaredMethods.first { it.name.startsWith("resetForTest") }
        method.isAccessible = true
        method.invoke(Switchboard)
    }

    @Test
    public fun testConvenienceInitWiresContextAndOverridesEndToEnd(): Unit = runBlocking {
        // Call top-level convenience builder first
        Switchboard.init(
            context = context,
            debugEnabled = true
        )
        // Await background async initialization workers
        Switchboard.awaitIdle()

        // Write override value using the storage delegate triggering reactive flow collections
        storage.write("androidIntegrationFlag", "true")

        // Allow background IO dispatcher flow collector coroutines to process the state emission
        kotlinx.coroutines.delay(100)

        // Resolve flag ensuring prioritized override evaluation wins
        Switchboard.resolveBoolean("androidIntegrationFlag", false) shouldBe true
    }
}
