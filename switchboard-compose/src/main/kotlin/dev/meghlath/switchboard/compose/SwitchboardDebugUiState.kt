package dev.meghlath.switchboard.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import dev.meghlath.switchboard.core.FlagType

/**
 * Indicates the resolved resolution layer source for a feature flag's active evaluation.
 */
@Stable
public enum class ValueSource {
    /** Evaluated locally via dynamic developer override. */
    OVERRIDE,
    /** Resolved via remote configuration backend provider. */
    REMOTE,
    /** Synchronous static fallback default annotated metadata. */
    DEFAULT
}

/**
 * Complete immutable visual representation of a single feature flag within the debug UI dashboard.
 *
 * @property key The unique identifier string.
 * @property type The underlying evaluated [FlagType].
 * @property defaultValue Fallback static string representation.
 * @property description Developer documentation note.
 * @property category Grouping domain classification.
 * @property enumEntries Ordered valid variant string values if [type] is [FlagType.ENUM].
 * @property currentValue Currently evaluated live value string representation.
 * @property source Evaluated layer [ValueSource] used to render column border color.
 */
@Immutable
public data class FlagUiModel(
    val key: String,
    val type: FlagType,
    val defaultValue: String,
    val description: String,
    val category: String,
    val enumEntries: List<String>,
    val currentValue: String,
    val source: ValueSource
)

/**
 * Immutable dashboard container wrapping complete screening state, selected filtering scopes,
 * and active evaluations.
 *
 * Optimized for Jetpack Compose stable recomposition boundary enforcement.
 *
 * @property isLoading Whether async baseline synchronization is in flight.
 * @property allFlags Master collection of evaluated interactive models.
 * @property searchQuery Active filtering string applied to key and description matching.
 * @property selectedCategory Selected category tab scope, or `null` for all categories.
 */
@Immutable
public data class SwitchboardDebugUiState(
    val isLoading: Boolean = true,
    val allFlags: List<FlagUiModel> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null
) {
    /**
     * Sorted set of discovered unique classification strings.
     */
    val categories: List<String>
        get() = allFlags.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted()

    /**
     * Active subset matching criteria combinations of category selection and search criteria.
     */
    val filteredFlags: List<FlagUiModel>
        get() {
            return allFlags.filter { flag ->
                val matchesCategory = selectedCategory == null || flag.category == selectedCategory
                val matchesSearch = searchQuery.isBlank() ||
                    flag.key.contains(searchQuery, ignoreCase = true) ||
                    flag.description.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }

    /**
     * Helper determining if explicit registry registration was omitted or no flags exist.
     */
    val isEmptyState: Boolean
        get() = !isLoading && allFlags.isEmpty()
}
