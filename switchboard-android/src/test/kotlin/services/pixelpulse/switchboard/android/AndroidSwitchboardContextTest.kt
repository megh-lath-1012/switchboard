package services.pixelpulse.switchboard.android

import android.content.Context
import services.pixelpulse.switchboard.core.OverrideStorage
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

public class AndroidSwitchboardContextTest {

    @Test
    public fun testDoesNotLeakActivityContext() {
        val appCtx = mockk<Context>()
        val activityCtx = mockk<Context>()
        val mockStorage = mockk<OverrideStorage>()

        // Using secondary constructor/internal DI factory confirms applicationContext is isolated
        val tempDir = java.nio.file.Files.createTempDirectory("context_test").toFile()
        java.io.File(tempDir, "datastore").mkdirs()
        every { activityCtx.applicationContext } returns appCtx
        every { appCtx.applicationContext } returns appCtx
        every { appCtx.filesDir } returns tempDir

        val context = AndroidSwitchboardContext(activityCtx)
        
        // Confirm constructor accesses applicationContext exactly once to wrap or assign delegates
        verify(exactly = 1) { activityCtx.applicationContext }
    }

    @Test
    public fun testInternalStorageDelegation() {
        val mockStorage = mockk<OverrideStorage>()
        val context = AndroidSwitchboardContext(mockStorage)

        context.overrideStorage() shouldBeSameInstanceAs mockStorage
    }
}
