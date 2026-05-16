package dev.meghlath.switchboard.core

/**
 * Enumeration representing the supported native types for Switchboard feature flags.
 */
public enum class FlagType {
    BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING, ENUM
}

/**
 * Model capturing complete runtime metadata for a single feature flag discovered by KSP.
 *
 * @property key The unique identifier string of the flag.
 * @property type The evaluated [FlagType].
 * @property defaultValue The string representation of the fallback default value.
 * @property description The developer-provided documentation string.
 * @property category The functional classification grouping.
 * @property enumEntries The ordered list of string names for enum entries if [type] is [FlagType.ENUM].
 */
public data class RegisteredFlag(
    val key: String,
    val type: FlagType,
    val defaultValue: String,
    val description: String,
    val category: String,
    val enumEntries: List<String> = emptyList()
)

/**
 * Interface defining a centralized catalog of registered feature flags.
 *
 * Typically implemented automatically by the `switchboard-ksp` processor (e.g., `SwitchboardRegistryImpl`).
 */
public interface SwitchboardRegistry {
    /**
     * The immutable list of all flag definitions discovered across annotated source containers.
     */
    public val flags: List<RegisteredFlag>
}

/**
 * Sentinel empty implementation of [SwitchboardRegistry] returned when explicit registration is omitted.
 */
public object EmptySwitchboardRegistry : SwitchboardRegistry {
    override val flags: List<RegisteredFlag> = emptyList()
}
