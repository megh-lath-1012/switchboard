package dev.meghlath.switchboard.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * Aggregator backend merging multiple distinct [Backend] instances into a unified prioritized source.
 *
 * Evaluation follows strict array order hierarchy: the first backend to return a non-null resolved value wins.
 * Emits reactive changes whenever any encapsulated backend stream fires.
 *
 * **Usage Example:**
 * ```kotlin
 * val composite = CompositeBackend(remoteBackend, localFallbackBackend)
 * ```
 *
 * @property backends The ordered collection of delegated backends.
 */
public class CompositeBackend(
    public vararg val backends: Backend
) : Backend {

    override suspend fun getBoolean(key: String): Boolean? {
        for (backend in backends) {
            val result = backend.getBoolean(key)
            if (result != null) return result
        }
        return null
    }

    override suspend fun getInt(key: String): Int? {
        for (backend in backends) {
            val result = backend.getInt(key)
            if (result != null) return result
        }
        return null
    }

    override suspend fun getLong(key: String): Long? {
        for (backend in backends) {
            val result = backend.getLong(key)
            if (result != null) return result
        }
        return null
    }

    override suspend fun getFloat(key: String): Float? {
        for (backend in backends) {
            val result = backend.getFloat(key)
            if (result != null) return result
        }
        return null
    }

    override suspend fun getDouble(key: String): Double? {
        for (backend in backends) {
            val result = backend.getDouble(key)
            if (result != null) return result
        }
        return null
    }

    override suspend fun getString(key: String): String? {
        for (backend in backends) {
            val result = backend.getString(key)
            if (result != null) return result
        }
        return null
    }

    override fun changes(): Flow<Unit> {
        return merge(*backends.map { it.changes() }.toTypedArray())
    }
}
