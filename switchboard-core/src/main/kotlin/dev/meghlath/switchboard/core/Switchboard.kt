package dev.meghlath.switchboard.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized runtime resolution engine for Switchboard feature flags.
 *
 * Manages evaluation hierarchy, volatile memory caching, background IO synchronization,
 * and reactive refresh lifecycles.
 *
 * **Resolution Priority Order:**
 * 1. **Local Overrides**: If `debugEnabled` is true and an override exists in storage.
 * 2. **Remote Backend**: If configured and returns a non-null compatible value.
 * 3. **Annotation Default**: Fallback value synchronously returned on cache misses or pre-init reads.
 *
 * **Usage Example:**
 * ```kotlin
 * Switchboard.init(context, backend, debugEnabled = true)
 * val timeout = Switchboard.resolveInt("apiTimeoutMs", 5000)
 * ```
 */
public object Switchboard {
    private var isInitialized = false
    private var context: SwitchboardContext? = null
    private var registry: SwitchboardRegistry = EmptySwitchboardRegistry
    private var backend: Backend = NoOpBackend
    private var debugEnabled: Boolean = false

    internal fun getActiveContext(): SwitchboardContext? = context
    internal fun getActiveRegistry(): SwitchboardRegistry = registry
    internal fun getActiveBackend(): Backend = backend
    internal fun isDebugEnabled(): Boolean = debugEnabled
    internal fun getActiveScope(): CoroutineScope? = scope
    internal fun getOverridesMap(): Map<String, String> = overridesMap
    internal fun getCachedValue(key: String): Any? = cache[key]

    private val preInitWarningLogged = AtomicBoolean(false)
    private val initWarningLogged = AtomicBoolean(false)

    private val cache = ConcurrentHashMap<String, Any>()
    private val overridesMap = ConcurrentHashMap<String, String>()
    private val inFlightReads = ConcurrentHashMap<String, Boolean>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private var scope: CoroutineScope? = null
    private var initJob: Job? = null

    /**
     * Registers a custom functional interceptor for internal Switchboard diagnostic logs.
     *
     * **Handler Lifecycle & Thread-Safety Semantics:**
     * - **Independent Lifecycle**: Callable at any time before OR after [Switchboard.init]. Logging operates completely decoupled from core initialization.
     * - **Thread-Safe Assignment**: Internally backed by a volatile reference allowing concurrent thread-safe replacement without blocking evaluation paths.
     * - **Last Call Wins**: Invoking this method replaces any previously registered handler immediately. Consecutive registrations do not chain, and double-setting does not trigger exceptions.
     * - **Default Restoration**: Passing `null` removes the current interceptor and restores the default standard output (`println`) logging router.
     * - **No Back-Buffering**: Diagnostics generated prior to registration are routed exclusively to the active handler at that moment. Historical logs are not buffered or replayed.
     *
     * @param handler The functional lambda to intercept log severities, or `null` to restore standard output routing.
     */
    public fun setLogHandler(handler: LogHandler?) {
        activeLogHandler = handler
    }

    /**
     * The currently active log handler, or null if using the default println-based handler.
     */
    public val logHandler: LogHandler? get() = activeLogHandler

    /**
     * Initializes the Switchboard runtime engine. Idempotent operation.
     *
     * Launches asynchronous synchronization of underlying storage engines without blocking the caller.
     *
     * @param context Platform capability context providing override storage access.
     * @param registry Centralized catalog containing metadata for all feature flags. Defaults to [EmptySwitchboardRegistry].
     * @param backend Configuration provider source. Defaults to [NoOpBackend].
     * @param debugEnabled Whether local overrides are evaluated. Defaults to `false`.
     */
    @Synchronized
    public fun init(
        context: SwitchboardContext,
        registry: SwitchboardRegistry = EmptySwitchboardRegistry,
        backend: Backend = NoOpBackend,
        debugEnabled: Boolean = false
    ) {
        if (isInitialized) {
            if (initWarningLogged.compareAndSet(false, true)) {
                currentLogger.warn("Switchboard.init() called more than once. Subsequent calls are ignored.")
            }
            return
        }
        this.context = context
        this.registry = registry
        this.backend = backend
        this.debugEnabled = debugEnabled
        this.isInitialized = true

        // Clear cache synchronously upon first initialization
        cache.clear()

        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        this.scope = coroutineScope

        synchronized(inFlightReads) {
            // Async wiring must not block the caller
            this.initJob = coroutineScope.launch {
                try {
                    val initialOverrides = context.overrideStorage().read()
                    overridesMap.clear()
                    overridesMap.putAll(initialOverrides)
                } catch (e: Exception) {
                    currentLogger.warn("Failed to read initial overrides: ${e.message}")
                }
            }
        }

        // Collect overrideStorage changes independently on scope
        coroutineScope.launch {
            try {
                context.overrideStorage().changes().collect { newOverrides ->
                    overridesMap.clear()
                    overridesMap.putAll(newOverrides)
                    cache.clear()
                }
            } catch (e: Exception) {
                currentLogger.warn("Error collecting overrideStorage changes: ${e.message}")
            }
        }

        // Collect backend changes independently on scope
        coroutineScope.launch {
            try {
                backend.changes().collect {
                    cache.clear()
                }
            } catch (e: Exception) {
                currentLogger.warn("Error collecting backend changes: ${e.message}")
            }
        }
    }

    /**
     * Resolves a boolean feature flag value synchronously.
     *
     * @param key The unique flag identifier.
     * @param default The fallback default value.
     * @return The prioritized resolved boolean value.
     */
    public fun resolveBoolean(key: String, default: Boolean): Boolean {
        if (!checkInitialized()) return default

        val cached = cache[key]
        if (cached != null) return cached as? Boolean ?: default

        if (debugEnabled) {
            val overrideStr = overridesMap[key]
            if (overrideStr != null) {
                val parsed = overrideStr.toBooleanStrictOrNull()
                if (parsed != null) {
                    cache[key] = parsed
                    return parsed
                } else {
                    currentLogger.warn("Override returned wrong type for key $key: expected Boolean but found '$overrideStr'")
                }
            }
        }

        triggerBackendFetch(key, default) { backend.getBoolean(key) }
        return default
    }

    /**
     * Resolves an integer feature flag value synchronously.
     *
     * @param key The unique flag identifier.
     * @param default The fallback default value.
     * @return The prioritized resolved integer value.
     */
    public fun resolveInt(key: String, default: Int): Int {
        if (!checkInitialized()) return default

        val cached = cache[key]
        if (cached != null) return cached as? Int ?: default

        if (debugEnabled) {
            val overrideStr = overridesMap[key]
            if (overrideStr != null) {
                val parsed = overrideStr.toIntOrNull()
                if (parsed != null) {
                    cache[key] = parsed
                    return parsed
                } else {
                    currentLogger.warn("Override returned wrong type for key $key: expected Int but found '$overrideStr'")
                }
            }
        }

        triggerBackendFetch(key, default) { backend.getInt(key) }
        return default
    }

    /**
     * Resolves a long feature flag value synchronously.
     *
     * @param key The unique flag identifier.
     * @param default The fallback default value.
     * @return The prioritized resolved long value.
     */
    public fun resolveLong(key: String, default: Long): Long {
        if (!checkInitialized()) return default

        val cached = cache[key]
        if (cached != null) return cached as? Long ?: default

        if (debugEnabled) {
            val overrideStr = overridesMap[key]
            if (overrideStr != null) {
                val parsed = overrideStr.toLongOrNull()
                if (parsed != null) {
                    cache[key] = parsed
                    return parsed
                } else {
                    currentLogger.warn("Override returned wrong type for key $key: expected Long but found '$overrideStr'")
                }
            }
        }

        triggerBackendFetch(key, default) { backend.getLong(key) }
        return default
    }

    /**
     * Resolves a float feature flag value synchronously.
     *
     * @param key The unique flag identifier.
     * @param default The fallback default value.
     * @return The prioritized resolved float value.
     */
    public fun resolveFloat(key: String, default: Float): Float {
        if (!checkInitialized()) return default

        val cached = cache[key]
        if (cached != null) return cached as? Float ?: default

        if (debugEnabled) {
            val overrideStr = overridesMap[key]
            if (overrideStr != null) {
                val parsed = overrideStr.toFloatOrNull()
                if (parsed != null) {
                    cache[key] = parsed
                    return parsed
                } else {
                    currentLogger.warn("Override returned wrong type for key $key: expected Float but found '$overrideStr'")
                }
            }
        }

        triggerBackendFetch(key, default) { backend.getFloat(key) }
        return default
    }

    /**
     * Resolves a double feature flag value synchronously.
     *
     * @param key The unique flag identifier.
     * @param default The fallback default value.
     * @return The prioritized resolved double value.
     */
    public fun resolveDouble(key: String, default: Double): Double {
        if (!checkInitialized()) return default

        val cached = cache[key]
        if (cached != null) return cached as? Double ?: default

        if (debugEnabled) {
            val overrideStr = overridesMap[key]
            if (overrideStr != null) {
                val parsed = overrideStr.toDoubleOrNull()
                if (parsed != null) {
                    cache[key] = parsed
                    return parsed
                } else {
                    currentLogger.warn("Override returned wrong type for key $key: expected Double but found '$overrideStr'")
                }
            }
        }

        triggerBackendFetch(key, default) { backend.getDouble(key) }
        return default
    }

    /**
     * Resolves a string feature flag value synchronously.
     *
     * @param key The unique flag identifier.
     * @param default The fallback default value.
     * @return The prioritized resolved string value.
     */
    public fun resolveString(key: String, default: String): String {
        if (!checkInitialized()) return default

        val cached = cache[key]
        if (cached != null) return cached as? String ?: default

        if (debugEnabled) {
            val overrideStr = overridesMap[key]
            if (overrideStr != null) {
                cache[key] = overrideStr
                return overrideStr
            }
        }

        triggerBackendFetch(key, default) { backend.getString(key) }
        return default
    }

    /**
     * Resolves an enum feature flag value synchronously.
     *
     * @param T The reified enum type.
     * @param key The unique flag identifier.
     * @param default The fallback default enum entry.
     * @return The prioritized resolved enum entry.
     */
    public inline fun <reified T : Enum<T>> resolveEnum(key: String, default: T): T {
        val resolvedString = resolveString(key, default.name)
        return try {
            enumValues<T>().firstOrNull { it.name == resolvedString } ?: default
        } catch (e: Exception) {
            default
        }
    }

    /**
     * Explicitly invalidates all cached flag evaluations.
     * Subsequent reads will re-evaluate from active storage or backend providers.
     */
    public fun refresh() {
        cache.clear()
    }

    /**
     * Suspends until all background initialization and active cache-fetch coroutines complete.
     * Designed to enable highly deterministic test verification blocks.
     */
    public suspend fun awaitIdle() {
        initJob?.join()
        val jobs = synchronized(inFlightReads) { activeJobs.values.toList() }
        jobs.forEach { it.join() }
    }

    /**
     * Clears all runtime engine state. Intended strictly for test cleanup lifecycles.
     */
    @Synchronized
    internal fun resetForTest() {
        initJob?.cancel()
        synchronized(inFlightReads) {
            activeJobs.values.forEach { it.cancel() }
            activeJobs.clear()
        }
        scope?.cancel()
        scope = null
        isInitialized = false
        context = null
        registry = EmptySwitchboardRegistry
        backend = NoOpBackend
        debugEnabled = false
        preInitWarningLogged.set(false)
        initWarningLogged.set(false)
        cache.clear()
        overridesMap.clear()
        inFlightReads.clear()
        activeLogHandler = null
    }

    private fun checkInitialized(): Boolean {
        if (!isInitialized) {
            if (preInitWarningLogged.compareAndSet(false, true)) {
                currentLogger.warn("Switchboard.resolveX() called before init() completed. Returning default values.")
            }
            return false
        }
        return true
    }

    private fun <T : Any> triggerBackendFetch(key: String, defaultVal: T, fetcher: suspend () -> T?) {
        val currentScope = scope ?: return
        synchronized(inFlightReads) {
            if (!inFlightReads.containsKey(key)) {
                inFlightReads[key] = true
                val job = currentScope.launch(Dispatchers.IO) {
                    try {
                        // Double check override isolation inside background worker
                        val overrideStr = if (debugEnabled) overridesMap[key] else null
                        if (overrideStr != null) {
                            val parsed = parseOverride(key, overrideStr, defaultVal)
                            if (parsed != null) {
                                cache[key] = parsed
                                return@launch
                            }
                        }

                        val backendVal = try {
                            fetcher()
                        } catch (e: Exception) {
                            if (e is ClassCastException || e.toString().contains("ClassCastException") || e.message?.contains("Incompatible types") == true) {
                                currentLogger.warn("Backend returned wrong type for key $key: ${e.message}")
                            } else {
                                currentLogger.warn("Backend evaluation failed for key $key: ${e.message}")
                            }
                            null
                        }

                        if (backendVal != null) {
                            cache[key] = backendVal
                        } else {
                            cache[key] = defaultVal
                        }
                    } finally {
                        synchronized(inFlightReads) {
                            inFlightReads.remove(key)
                            activeJobs.remove(key)
                        }
                    }
                }
                activeJobs[key] = job
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> parseOverride(key: String, overrideStr: String, defaultVal: T): T? {
        return when (defaultVal) {
            is Boolean -> {
                val res = overrideStr.toBooleanStrictOrNull()
                if (res == null) currentLogger.warn("Override returned wrong type for key $key: expected Boolean but found '$overrideStr'")
                res as? T
            }
            is Int -> {
                val res = overrideStr.toIntOrNull()
                if (res == null) currentLogger.warn("Override returned wrong type for key $key: expected Int but found '$overrideStr'")
                res as? T
            }
            is Long -> {
                val res = overrideStr.toLongOrNull()
                if (res == null) currentLogger.warn("Override returned wrong type for key $key: expected Long but found '$overrideStr'")
                res as? T
            }
            is Float -> {
                val res = overrideStr.toFloatOrNull()
                if (res == null) currentLogger.warn("Override returned wrong type for key $key: expected Float but found '$overrideStr'")
                res as? T
            }
            is Double -> {
                val res = overrideStr.toDoubleOrNull()
                if (res == null) currentLogger.warn("Override returned wrong type for key $key: expected Double but found '$overrideStr'")
                res as? T
            }
            is String -> overrideStr as? T
            else -> overrideStr as? T
        }
    }
}
