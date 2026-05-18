package services.pixelpulse.switchboard.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Inert default backend implementation returning unconfigured `null` values for all reads.
 *
 * Emits an empty non-triggering stream for reactive change monitors.
 *
 * **Usage Example:**
 * ```kotlin
 * val backend: Backend = NoOpBackend
 * ```
 */
public object NoOpBackend : Backend {
    override suspend fun getBoolean(key: String): Boolean? = null
    override suspend fun getInt(key: String): Int? = null
    override suspend fun getLong(key: String): Long? = null
    override suspend fun getFloat(key: String): Float? = null
    override suspend fun getDouble(key: String): Double? = null
    override suspend fun getString(key: String): String? = null
    override fun changes(): Flow<Unit> = emptyFlow()
}
