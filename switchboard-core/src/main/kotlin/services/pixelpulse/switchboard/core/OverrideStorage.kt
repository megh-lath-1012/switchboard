package services.pixelpulse.switchboard.core

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic storage abstraction for persistent feature flag overrides.
 *
 * Implementations are responsible for persisting key-value string pairs securely
 * and efficiently (e.g., via Jetpack DataStore on Android or file storage on JVM).
 *
 * **Usage Example:**
 * ```kotlin
 * val overrides = overrideStorage.read()
 * overrideStorage.write("enableNewHome", "true")
 * ```
 */
public interface OverrideStorage {
    /**
     * Reads all currently persisted flag overrides.
     *
     * @return A map of flag keys to their overridden string values.
     */
    public suspend fun read(): Map<String, String>

    /**
     * Persists or removes an override for a specific flag key.
     *
     * @param key The unique key of the feature flag.
     * @param value The string representation of the overridden value, or `null` to remove the override.
     */
    public suspend fun write(key: String, value: String?)

    /**
     * Clears all persisted overrides.
     */
    public suspend fun clear()

    /**
     * A reactive stream emitting the current map of overrides whenever changes occur.
     *
     * @return A [Flow] emitting the complete override map upon modifications.
     */
    public fun changes(): Flow<Map<String, String>>
}
