package services.pixelpulse.switchboard.sample

import services.pixelpulse.switchboard.core.Backend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class LocalFakeFirebaseBackend : Backend {
    override suspend fun getBoolean(key: String): Boolean? = null

    override suspend fun getInt(key: String): Int? = null

    override suspend fun getLong(key: String): Long? = null

    override suspend fun getFloat(key: String): Float? {
        if (key == "autoDiscountPercent") return 0.10f
        return null
    }

    override suspend fun getDouble(key: String): Double? = null

    override suspend fun getString(key: String): String? {
        if (key == "greetingText") return "Welcome back ✨"
        return null
    }

    override fun changes(): Flow<Unit> = emptyFlow()
}
