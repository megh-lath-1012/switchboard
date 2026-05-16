package dev.meghlath.switchboard.core

import kotlinx.coroutines.flow.Flow

/**
 * Common configuration provider interface interface responsible for fetching resolved feature flag values.
 *
 * Backends represent disparate configuration sources such as remote cloud providers, local disk overrides,
 * or mock data harnesses.
 *
 * **Suspend Asymmetry Note:**
 * Flag value accessors (`getBoolean`, `getInt`, etc.) are declared as `suspend` functions to natively accommodate
 * backends that perform real asynchronous disk operations or network API calls. In contrast, testing backends
 * such as [InMemoryBackend] expose standard synchronous, non-suspend setters since they manipulate internal
 * volatile memory caches instantly.
 *
 * **Usage Example:**
 * ```kotlin
 * val enabled = backend.getBoolean("enableNewHome")
 * ```
 */
public interface Backend {
    /**
     * Resolves a boolean feature flag value by its key.
     *
     * @param key The unique key string identifier of the flag.
     * @return The configured boolean value, or `null` if unconfigured or incompatible.
     */
    public suspend fun getBoolean(key: String): Boolean?

    /**
     * Resolves an integer feature flag value by its key.
     *
     * @param key The unique key string identifier of the flag.
     * @return The configured integer value, or `null` if unconfigured or incompatible.
     */
    public suspend fun getInt(key: String): Int?

    /**
     * Resolves a long feature flag value by its key.
     *
     * @param key The unique key string identifier of the flag.
     * @return The configured long value, or `null` if unconfigured or incompatible.
     */
    public suspend fun getLong(key: String): Long?

    /**
     * Resolves a floating-point feature flag value by its key.
     *
     * @param key The unique key string identifier of the flag.
     * @return The configured float value, or `null` if unconfigured or incompatible.
     */
    public suspend fun getFloat(key: String): Float?

    /**
     * Resolves a double-precision feature flag value by its key.
     *
     * @param key The unique key string identifier of the flag.
     * @return The configured double value, or `null` if unconfigured or incompatible.
     */
    public suspend fun getDouble(key: String): Double?

    /**
     * Resolves a raw string feature flag value by its key.
     *
     * @param key The unique key string identifier of the flag.
     * @return The configured string value, or `null` if unconfigured.
     */
    public suspend fun getString(key: String): String?

    /**
     * A reactive notification stream signaling when underlying configuration values are updated.
     *
     * @return A [Flow] emitting unit tokens whenever backend configurations refresh.
     */
    public fun changes(): Flow<Unit>
}
