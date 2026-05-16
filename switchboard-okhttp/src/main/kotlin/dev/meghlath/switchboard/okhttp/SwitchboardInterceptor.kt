package dev.meghlath.switchboard.okhttp

import dev.meghlath.switchboard.annotations.InternalSwitchboardApi
import dev.meghlath.switchboard.core.LogLevel
import dev.meghlath.switchboard.core.Switchboard
import dev.meghlath.switchboard.core.SwitchboardDebugAccessor
import dev.meghlath.switchboard.core.LogHandler
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Granularity of feature flag reporting in network logs.
 */
public enum class SwitchboardLogLevel { 
    /** Logs state for all registered feature flags. */
    ALL, 
    /** Only logs flags that currently have a local debug override active. */
    OVERRIDES_ONLY 
}

/**
 * OkHttp [Interceptor] that appends feature flag resolution metadata to outgoing requests.
 * 
 * Logs are routed via [Switchboard.logHandler] at [LogLevel.DEBUG] level.
 * In production builds (where debugEnabled is false), this interceptor is a no-op.
 */
public class SwitchboardInterceptor(
    public val logLevel: SwitchboardLogLevel = SwitchboardLogLevel.OVERRIDES_ONLY
) : Interceptor {

    @OptIn(InternalSwitchboardApi::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Pass through immediately if debug capabilities are disabled
        if (!SwitchboardDebugAccessor.isDebugEnabled()) {
            return chain.proceed(request)
        }

        try {
            val registry = SwitchboardDebugAccessor.getRegistry()
            val flagsToLog = mutableListOf<String>()

            for (flag in registry.flags) {
                val override = SwitchboardDebugAccessor.getOverride(flag.key)
                val source: String
                val value: String

                if (override != null) {
                    source = "OVERRIDE ⚠️"
                    value = override
                } else {
                    val backendValue = SwitchboardDebugAccessor.getBackendValue(flag.key, flag.type)
                    if (backendValue != null) {
                        source = "REMOTE"
                        value = backendValue
                    } else {
                        source = "DEFAULT"
                        value = flag.defaultValue
                    }
                }

                val shouldLog = when (logLevel) {
                    SwitchboardLogLevel.ALL -> true
                    SwitchboardLogLevel.OVERRIDES_ONLY -> override != null
                }

                if (shouldLog) {
                    flagsToLog.add("  ↳ ${flag.key}: $value [$source]")
                }
            }

            if (flagsToLog.isNotEmpty()) {
                val method = request.method
                val url = request.url.toString()
                val header = "Switchboard ▶ [$method] $url"
                val lines = mutableListOf<String>()
                lines.add(header)
                lines.addAll(flagsToLog)
                
                val fullLog = lines.joinToString("\n")
                val handler: LogHandler? = Switchboard.logHandler
                handler?.invoke(LogLevel.DEBUG, fullLog, null)
            }
        } catch (e: Exception) {
            val handler: LogHandler? = Switchboard.logHandler
            handler?.invoke(LogLevel.WARN, "SwitchboardInterceptor: Error resolving flags", e)
        }

        return chain.proceed(request)
    }
}
