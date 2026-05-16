@file:Suppress("DEPRECATION") // createComposeRule v1 API used for Roborazzi compatibility

package dev.meghlath.switchboard.compose

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import dev.meghlath.switchboard.core.FlagType
import dev.meghlath.switchboard.core.InMemoryBackend
import dev.meghlath.switchboard.core.OverrideStorage
import dev.meghlath.switchboard.core.RegisteredFlag
import dev.meghlath.switchboard.core.Switchboard
import dev.meghlath.switchboard.core.SwitchboardContext
import dev.meghlath.switchboard.core.SwitchboardRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
public class SwitchboardDebugScreenScreenshotTest {

    @get:Rule
    public val composeTestRule = createComposeRule()

    private class LocalFakeOverrideStorage : OverrideStorage {
        private val map = mutableMapOf<String, String>()
        private val flow = MutableSharedFlow<Map<String, String>>(replay = 1)

        init { flow.tryEmit(emptyMap()) }
        override suspend fun read(): Map<String, String> = map.toMap()
        override suspend fun write(key: String, value: String?) {
            if (value != null) map[key] = value else map.remove(key)
            flow.tryEmit(map.toMap())
        }
        override suspend fun clear() {
            map.clear()
            flow.tryEmit(map.toMap())
        }
        override fun changes(): Flow<Map<String, String>> = flow.asSharedFlow()

        fun setOverrideSync(key: String, value: String) {
            map[key] = value
            flow.tryEmit(map.toMap())
        }
    }

    private class LocalFakeContext(val storage: LocalFakeOverrideStorage) : SwitchboardContext {
        override fun overrideStorage(): OverrideStorage = storage
    }

    private object TestRegistry : SwitchboardRegistry {
        override val flags: List<RegisteredFlag> = listOf(
            RegisteredFlag(
                key = "fx_bool_flag",
                type = FlagType.BOOLEAN,
                defaultValue = "true",
                description = "Enable awesome new UI feature",
                category = "Core"
            ),
            RegisteredFlag(
                key = "fx_timeout_ms",
                type = FlagType.INT,
                defaultValue = "5000",
                description = "Network timeout window",
                category = "Network"
            ),
            RegisteredFlag(
                key = "fx_theme_mode",
                type = FlagType.ENUM,
                defaultValue = "DARK",
                description = "Application color theme layout",
                category = "UI",
                enumEntries = listOf("LIGHT", "DARK", "SYSTEM")
            )
        )
    }

    private lateinit var holderScope: CoroutineScope

    private fun resetSwitchboard() {
        val resetMethod = Switchboard::class.java.declaredMethods.find { it.name.startsWith("resetForTest") }
        if (resetMethod != null) {
            resetMethod.isAccessible = true
            resetMethod.invoke(Switchboard)
        }
    }

    @Before
    public fun setUp() {
        resetSwitchboard()
        holderScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    public fun tearDown() {
        holderScope.cancel()
        resetSwitchboard()
    }

    @Test
    public fun capturePopulatedDashboardScreen(): Unit = runBlocking {
        Switchboard.init(
            context = LocalFakeContext(LocalFakeOverrideStorage()),
            registry = TestRegistry,
            backend = InMemoryBackend(),
            debugEnabled = true
        )
        Switchboard.awaitIdle()

        val stateHolder = SwitchboardDebugStateHolder(coroutineScope = holderScope)
        delay(200) // Allow state engine to settle

        composeTestRule.setContent {
            SwitchboardDebugScreen(stateHolder = stateHolder)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    public fun captureEmptyStateScreen(): Unit = runBlocking {
        Switchboard.init(
            context = LocalFakeContext(LocalFakeOverrideStorage()),
            backend = InMemoryBackend(),
            debugEnabled = true
        )
        Switchboard.awaitIdle()

        val stateHolder = SwitchboardDebugStateHolder(coroutineScope = holderScope)
        delay(200)

        composeTestRule.setContent {
            SwitchboardDebugScreen(stateHolder = stateHolder)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    public fun captureActiveOverrideScreen(): Unit = runBlocking {
        val storage = LocalFakeOverrideStorage()
        // Pre-seed an override so the state holder picks it up immediately
        storage.setOverrideSync("fx_bool_flag", "false")

        Switchboard.init(
            context = LocalFakeContext(storage),
            registry = TestRegistry,
            backend = InMemoryBackend(),
            debugEnabled = true
        )
        Switchboard.awaitIdle()

        val stateHolder = SwitchboardDebugStateHolder(coroutineScope = holderScope)
        delay(300) // Allow override propagation through reactive Flow

        composeTestRule.setContent {
            SwitchboardDebugScreen(stateHolder = stateHolder)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    public fun captureSearchFilteredScreen(): Unit = runBlocking {
        Switchboard.init(
            context = LocalFakeContext(LocalFakeOverrideStorage()),
            registry = TestRegistry,
            backend = InMemoryBackend(),
            debugEnabled = true
        )
        Switchboard.awaitIdle()

        val stateHolder = SwitchboardDebugStateHolder(coroutineScope = holderScope)
        delay(200) // Allow state engine to settle

        // Apply search filter to narrow visible flags
        stateHolder.updateSearchQuery("timeout")
        delay(100) // Allow state recomputation

        composeTestRule.setContent {
            SwitchboardDebugScreen(stateHolder = stateHolder)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage()
    }
}
