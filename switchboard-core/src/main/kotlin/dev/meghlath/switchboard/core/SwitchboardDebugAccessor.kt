package dev.meghlath.switchboard.core

import dev.meghlath.switchboard.annotations.InternalSwitchboardApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/**
 * Internal-only gateway exposing isolated debug capabilities and raw storage channels
 * to the `switchboard-compose` debug UI module.
 *
 * Annotated with [InternalSwitchboardApi] to strictly protect stable public library surfaces
 * from unauthorized external consumption.
 */
@InternalSwitchboardApi
public object SwitchboardDebugAccessor {

    /**
     * Retrieves the centralized feature flag registry populated during engine initialization.
     */
    public fun getRegistry(): SwitchboardRegistry {
        return Switchboard.getActiveRegistry()
    }

    /**
     * Retrieves the raw string representation of a remote feature flag value by querying
     * the active backend provider directly.
     *
     * @param key The flag identifier string.
     * @param type The underlying data type expected.
     * @return The evaluated remote string, or `null` if unconfigured.
     */
    public suspend fun getRawRemoteValue(key: String, type: FlagType): String? {
        val backend = Switchboard.getActiveBackend()
        return try {
            when (type) {
                FlagType.BOOLEAN -> backend.getBoolean(key)?.toString()
                FlagType.INT -> backend.getInt(key)?.toString()
                FlagType.LONG -> backend.getLong(key)?.toString()
                FlagType.FLOAT -> backend.getFloat(key)?.toString()
                FlagType.DOUBLE -> backend.getDouble(key)?.toString()
                FlagType.STRING, FlagType.ENUM -> backend.getString(key)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Observes live local debug overrides directly from the underlying storage layer.
     *
     * **Security Requirement:** If `debugEnabled` is `false`, this method returns [emptyFlow]
     * to protect production environments from exposing dynamic configurations.
     */
    public fun observeOverrides(): Flow<Map<String, String>> {
        if (!Switchboard.isDebugEnabled()) {
            return emptyFlow()
        }
        val context = Switchboard.getActiveContext() ?: return emptyFlow()
        return try {
            context.overrideStorage().changes()
        } catch (e: Exception) {
            emptyFlow()
        }
    }

    /**
     * Writes or removes a feature flag debug override.
     *
     * **Security Requirement:** If `debugEnabled` is `false`, this method logs a diagnostic warning
     * and performs no mutations to ensure production release integrity.
     *
     * @param key The unique flag identifier string.
     * @param value The raw string override representation, or `null` to clear the override.
     */
    public fun setOverride(key: String, value: String?) {
        if (!Switchboard.isDebugEnabled()) {
            currentLogger.warn("Attempted to set override for key '$key' while debugEnabled is false. Operation ignored.")
            return
        }
        val context = Switchboard.getActiveContext()
        if (context == null) {
            currentLogger.warn("Attempted to set override for key '$key' before Switchboard.init() completed. Operation ignored.")
            return
        }
        val scope = Switchboard.getActiveScope()
        if (scope != null) {
            scope.launch {
                try {
                    context.overrideStorage().write(key, value)
                } catch (e: Exception) {
                    currentLogger.warn("Failed to write override for key '$key': ${e.message}")
                }
            }
        } else {
            // Synchronous fallback or logging if scope is inactive
            currentLogger.warn("Attempted to set override for key '$key' with inactive coroutine scope.")
        }
    }

    /**
     * Checks if debug mode is active in the Switchboard engine.
     */
    public fun isDebugEnabled(): Boolean = Switchboard.isDebugEnabled()

    /**
     * Retrieves the current local override for a specific flag, if any.
     */
    public fun getOverride(key: String): String? {
        if (!Switchboard.isDebugEnabled()) return null
        return Switchboard.getOverridesMap()[key]
    }

    /**
     * Synchronously resolves a remote value from the backend for auditing purposes.
     * Use with caution as this blocks the calling thread.
     */
    @Suppress("DEPRECATION")
    public fun getBackendValue(key: String, type: FlagType): String? {
        return kotlinx.coroutines.runBlocking {
            getRawRemoteValue(key, type)
        }
    }
}
