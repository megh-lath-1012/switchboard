package services.pixelpulse.switchboard.compose

import app.cash.turbine.test
import services.pixelpulse.switchboard.core.FlagType
import services.pixelpulse.switchboard.core.InMemoryBackend
import services.pixelpulse.switchboard.core.OverrideStorage
import services.pixelpulse.switchboard.core.RegisteredFlag
import services.pixelpulse.switchboard.core.Switchboard
import services.pixelpulse.switchboard.core.SwitchboardContext
import services.pixelpulse.switchboard.core.SwitchboardRegistry
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

public class SwitchboardDebugStateHolderTest {

    private class LocalFakeOverrideStorage : OverrideStorage {
        private val map = mutableMapOf<String, String>()
        private val flow = MutableSharedFlow<Map<String, String>>(replay = 1)

        init {
            flow.tryEmit(emptyMap())
        }

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

        fun setOverrideSync(key: String, value: String?) {
            if (value != null) map[key] = value else map.remove(key)
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

    private lateinit var fakeStorage: LocalFakeOverrideStorage
    private lateinit var holderScope: CoroutineScope

    @BeforeEach
    public fun setUp(): Unit = runBlocking {
        fakeStorage = LocalFakeOverrideStorage()
        // Reset singleton to guarantee isolated boundary
        val resetMethod = Switchboard::class.java.declaredMethods.find { it.name.startsWith("resetForTest") }
        if (resetMethod != null) {
            resetMethod.isAccessible = true
            resetMethod.invoke(Switchboard)
        }

        Switchboard.init(
            context = LocalFakeContext(fakeStorage),
            registry = TestRegistry,
            backend = InMemoryBackend(),
            debugEnabled = true
        )
        Switchboard.awaitIdle()
        holderScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterEach
    public fun tearDown() {
        holderScope.cancel()
        val resetMethod = Switchboard::class.java.declaredMethods.find { it.name.startsWith("resetForTest") }
        if (resetMethod != null) {
            resetMethod.isAccessible = true
            resetMethod.invoke(Switchboard)
        }
    }

    @Test
    public fun testInitialBaselineLoadProducesCorrectModelsAndCategories(): Unit = runBlocking {
        val holder = SwitchboardDebugStateHolder(coroutineScope = holderScope)

        holder.state.test {
            // Await emission where state is loaded
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            current.isLoading shouldBe false
            current.allFlags shouldHaveSize 3
            current.categories shouldBe listOf("Core", "Network", "UI")

            val boolFlag = current.allFlags.find { it.key == "fx_bool_flag" }!!
            boolFlag.currentValue shouldBe "true"
            boolFlag.source shouldBe ValueSource.DEFAULT
        }
    }

    @Test
    public fun testSearchQueryFiltersKeysAndDescriptionsCorrectly(): Unit = runBlocking {
        val holder = SwitchboardDebugStateHolder(coroutineScope = holderScope)

        holder.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            holder.updateSearchQuery("timeout")
            val filteredByKey = awaitItem()
            filteredByKey.filteredFlags shouldHaveSize 1
            filteredByKey.filteredFlags.first().key shouldBe "fx_timeout_ms"

            holder.updateSearchQuery("awesome")
            val filteredByDesc = awaitItem()
            filteredByDesc.filteredFlags shouldHaveSize 1
            filteredByDesc.filteredFlags.first().key shouldBe "fx_bool_flag"
        }
    }

    @Test
    public fun testCategoryTabSelectionFiltersCorrectly(): Unit = runBlocking {
        val holder = SwitchboardDebugStateHolder(coroutineScope = holderScope)

        holder.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            holder.selectCategory("UI")
            val uiOnly = awaitItem()
            uiOnly.filteredFlags shouldHaveSize 1
            uiOnly.filteredFlags.first().key shouldBe "fx_theme_mode"

            holder.selectCategory(null)
            val all = awaitItem()
            all.filteredFlags shouldHaveSize 3
        }
    }

    @Test
    public fun testSetOverrideUpdatesStateSourceAndValue(): Unit = runBlocking {
        val holder = SwitchboardDebugStateHolder(coroutineScope = holderScope)

        holder.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            // Perform direct storage mutation to simulate persistent update stream
            fakeStorage.setOverrideSync("fx_bool_flag", "false")

            // Await invalidation propagation mapping update
            var updated = awaitItem()
            while (updated.allFlags.find { it.key == "fx_bool_flag" }?.currentValue == "true") {
                updated = awaitItem()
            }

            val flag = updated.allFlags.find { it.key == "fx_bool_flag" }!!
            flag.currentValue shouldBe "false"
            flag.source shouldBe ValueSource.OVERRIDE
        }
    }
}
