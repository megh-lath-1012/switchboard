package services.pixelpulse.switchboard.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe volatile memory backend implementation designed for automated testing harnesses
 * and rapid prototyping verification.
 *
 * Exposes non-suspend typed setters allowing synchronous test arrangement without blocking coroutines.
 * Emits reactive refresh tokens instantly upon any modification.
 *
 * **Usage Example:**
 * ```kotlin
 * val backend = InMemoryBackend()
 * backend.setBoolean("enableNewHome", true)
 * ```
 */
public class InMemoryBackend : Backend {
    private val storage = ConcurrentHashMap<String, Any>()
    private val changesFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun getBoolean(key: String): Boolean? = storage[key] as? Boolean
    override suspend fun getInt(key: String): Int? = storage[key] as? Int
    override suspend fun getLong(key: String): Long? = storage[key] as? Long
    override suspend fun getFloat(key: String): Float? = storage[key] as? Float
    override suspend fun getDouble(key: String): Double? = storage[key] as? Double
    override suspend fun getString(key: String): String? = storage[key] as? String

    override fun changes(): Flow<Unit> = changesFlow.asSharedFlow()

    /**
     * Synchronously sets or clears a boolean feature flag value.
     *
     * @param key The unique string identifier.
     * @param value The target value, or `null` to clear.
     */
    public fun setBoolean(key: String, value: Boolean?) {
        update(key, value)
    }

    /**
     * Synchronously sets or clears an integer feature flag value.
     *
     * @param key The unique string identifier.
     * @param value The target value, or `null` to clear.
     */
    public fun setInt(key: String, value: Int?) {
        update(key, value)
    }

    /**
     * Synchronously sets or clears a long feature flag value.
     *
     * @param key The unique string identifier.
     * @param value The target value, or `null` to clear.
     */
    public fun setLong(key: String, value: Long?) {
        update(key, value)
    }

    /**
     * Synchronously sets or clears a floating-point feature flag value.
     *
     * @param key The unique string identifier.
     * @param value The target value, or `null` to clear.
     */
    public fun setFloat(key: String, value: Float?) {
        update(key, value)
    }

    /**
     * Synchronously sets or clears a double-precision feature flag value.
     *
     * @param key The unique string identifier.
     * @param value The target value, or `null` to clear.
     */
    public fun setDouble(key: String, value: Double?) {
        update(key, value)
    }

    /**
     * Synchronously sets or clears a string feature flag value.
     *
     * @param key The unique string identifier.
     * @param value The target value, or `null` to clear.
     */
    public fun setString(key: String, value: String?) {
        update(key, value)
    }

    /**
     * Clears all configured volatile flags instantly.
     */
    public fun clear() {
        storage.clear()
        changesFlow.tryEmit(Unit)
    }

    private fun update(key: String, value: Any?) {
        if (value != null) {
            storage[key] = value
        } else {
            storage.remove(key)
        }
        changesFlow.tryEmit(Unit)
    }
}
