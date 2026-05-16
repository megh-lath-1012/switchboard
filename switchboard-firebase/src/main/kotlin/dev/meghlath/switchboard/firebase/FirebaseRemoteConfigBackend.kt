package dev.meghlath.switchboard.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import dev.meghlath.switchboard.core.Backend
import dev.meghlath.switchboard.core.LogLevel
import dev.meghlath.switchboard.core.Switchboard
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Switchboard [Backend] powered by Firebase Remote Config.
 *
 * Values are retrieved as strings from Firebase and coerced into the requested
 * types. If coercion fails, a warning is logged via [Switchboard.logHandler]
 * and null is returned, allowing the registry to fall back to default values.
 */
public class FirebaseRemoteConfigBackend(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
) : Backend {

    override suspend fun getBoolean(key: String): Boolean? = 
        coerce(key) { it.lowercase().toBooleanStrictOrNull() }

    override suspend fun getInt(key: String): Int? = 
        coerce(key) { it.toIntOrNull() }

    override suspend fun getLong(key: String): Long? = 
        coerce(key) { it.toLongOrNull() }

    override suspend fun getFloat(key: String): Float? = 
        coerce(key) { it.toFloatOrNull() }

    override suspend fun getDouble(key: String): Double? = 
        coerce(key) { it.toDoubleOrNull() }

    override suspend fun getString(key: String): String? = 
        remoteConfig.getString(key).takeIf { it.isNotEmpty() }

    override fun changes(): Flow<Unit> = callbackFlow {
        val listener = object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                trySend(Unit)
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Switchboard.logHandler?.invoke(
                    LogLevel.WARN,
                    "FirebaseRemoteConfigBackend: Config update listener error",
                    error
                )
            }
        }
        
        val registration = remoteConfig.addOnConfigUpdateListener(listener)
        
        awaitClose {
            registration.remove()
        }
    }

    private fun <T> coerce(key: String, converter: (String) -> T?): T? {
        val value = remoteConfig.getString(key)
        // Firebase returns empty string for missing keys if no defaults are set
        if (value.isEmpty()) return null
        
        val converted = converter(value)
        if (converted == null) {
            Switchboard.logHandler?.invoke(
                LogLevel.WARN,
                "FirebaseRemoteConfigBackend: Failed to coerce value '$value' for key '$key'",
                null
            )
        }
        return converted
    }
}
