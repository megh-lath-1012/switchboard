package services.pixelpulse.switchboard.compose

import services.pixelpulse.switchboard.annotations.InternalSwitchboardApi
import services.pixelpulse.switchboard.core.RegisteredFlag
import services.pixelpulse.switchboard.core.SwitchboardDebugAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Platform-independent logic controller governing feature flag metadata retrieval,
 * remote value evaluation aggregation, dynamic override ingestion streams, and view interactions.
 *
 * Designed as a pure Kotlin state engine decoupled from Android ViewModels to guarantee
 * maximum portability, synchronous JVM testability, and isolated reactive Flow boundaries.
 */
@OptIn(InternalSwitchboardApi::class)
public class SwitchboardDebugStateHolder(
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val _state = MutableStateFlow(SwitchboardDebugUiState(isLoading = true))
    /**
     * Complete stream of pre-computed stable view dashboard representations.
     */
    public val state: StateFlow<SwitchboardDebugUiState> = _state.asStateFlow()

    private val remoteValues = ConcurrentHashMap<String, String>()
    private var currentOverrides = emptyMap<String, String>()
    private var searchQuery = ""
    private var selectedCategory: String? = null

    init {
        val registeredFlags = SwitchboardDebugAccessor.getRegistry().flags

        // Trigger remote evaluation background workers concurrently per-key
        coroutineScope.launch {
            registeredFlags.forEach { flag ->
                launch {
                    val remoteVal = SwitchboardDebugAccessor.getRawRemoteValue(flag.key, flag.type)
                    if (remoteVal != null) {
                        remoteValues[flag.key] = remoteVal
                        recomputeState(registeredFlags, isLoading = false)
                    }
                }
            }
            recomputeState(registeredFlags, isLoading = false)
        }

        // Collect disk-level storage events to propagate real-time interface invalidations
        coroutineScope.launch {
            SwitchboardDebugAccessor.observeOverrides().collect { overrides ->
                currentOverrides = overrides
                recomputeState(registeredFlags, isLoading = false)
            }
        }
    }

    @Synchronized
    private fun recomputeState(registeredFlags: List<RegisteredFlag>, isLoading: Boolean) {
        val flagUiModels = registeredFlags.map { flag ->
            val overrideVal = currentOverrides[flag.key]
            val remoteVal = remoteValues[flag.key]

            val (currentVal, source) = when {
                overrideVal != null -> overrideVal to ValueSource.OVERRIDE
                remoteVal != null -> remoteVal to ValueSource.REMOTE
                else -> flag.defaultValue to ValueSource.DEFAULT
            }

            FlagUiModel(
                key = flag.key,
                type = flag.type,
                defaultValue = flag.defaultValue,
                description = flag.description,
                category = flag.category,
                enumEntries = flag.enumEntries,
                currentValue = currentVal,
                source = source
            )
        }

        _state.value = SwitchboardDebugUiState(
            isLoading = _state.value.isLoading && isLoading,
            allFlags = flagUiModels,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory
        )
    }

    /**
     * Updates active searching text query spanning keys and developer descriptions.
     *
     * @param query The partial text sequence to filter against.
     */
    public fun updateSearchQuery(query: String) {
        searchQuery = query
        _state.update { it.copy(searchQuery = query) }
    }

    /**
     * Scopes interface listing tabs to a specific classification domain.
     *
     * @param category Exact classification string, or `null` to view all categories.
     */
    public fun selectCategory(category: String?) {
        selectedCategory = category
        _state.update { it.copy(selectedCategory = category) }
    }

    /**
     * Dispatches volatile local overrides to underlying persistence modules.
     *
     * @param key The feature flag configuration key string.
     * @param value The raw target representation string, or `null` to restore remote/fallback inheritance.
     */
    public fun setOverride(key: String, value: String?) {
        SwitchboardDebugAccessor.setOverride(key, value)
    }

    /**
     * Purges all active custom test overrides across the discovery scope.
     */
    public fun clearAllOverrides() {
        currentOverrides.keys.forEach { key ->
            SwitchboardDebugAccessor.setOverride(key, null)
        }
    }
}
