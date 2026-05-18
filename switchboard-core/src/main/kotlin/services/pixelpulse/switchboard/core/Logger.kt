package services.pixelpulse.switchboard.core

/**
 * Diagnostic logging severity tiers for runtime evaluation reporting.
 */
public enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Functional hook signature for intercepting internal Switchboard diagnostics.
 *
 * @param level The severity classification of the diagnostic event.
 * @param message Descriptive text associated with the execution state.
 * @param throwable Optional exception context if evaluation errors occurred.
 */
public typealias LogHandler = (level: LogLevel, message: String, throwable: Throwable?) -> Unit

/**
 * Volatile storage retaining the active consumer log interceptor.
 */
@Volatile
internal var activeLogHandler: LogHandler? = null

/**
 * Internal logging abstraction to keep core KMP-isolated from platform-specific logging engines.
 */
internal interface Logger {
    /**
     * Logs a diagnostic warning message.
     *
     * @param message The text to log.
     */
    fun warn(message: String)

    /**
     * Logs an informational message.
     *
     * @param message The text to log.
     */
    fun info(message: String)
}

/**
 * Default standard output logger routing messages to the registered functional handler or falling back to `println`.
 */
internal object DefaultLogger : Logger {
    override fun warn(message: String) {
        val handler = activeLogHandler
        if (handler != null) {
            handler(LogLevel.WARN, message, null)
        } else {
            println("[Switchboard WARN]: $message")
        }
    }

    override fun info(message: String) {
        val handler = activeLogHandler
        if (handler != null) {
            handler(LogLevel.INFO, message, null)
        } else {
            println("[Switchboard INFO]: $message")
        }
    }
}

/**
 * Global accessible logger instance configurable by internal test suites.
 */
internal var currentLogger: Logger = DefaultLogger
