package dev.meghlath.switchboard.android

import android.util.Log
import dev.meghlath.switchboard.core.LogHandler
import dev.meghlath.switchboard.core.LogLevel

/**
 * Functional Android logging router mapping Switchboard core diagnostic severities
 * directly to native `android.util.Log` channels under tag `"Switchboard"`.
 */
public object AndroidLogger : LogHandler {
    override fun invoke(level: LogLevel, message: String, throwable: Throwable?) {
        if (throwable != null) {
            when (level) {
                LogLevel.DEBUG -> Log.d("Switchboard", message, throwable)
                LogLevel.INFO -> Log.i("Switchboard", message, throwable)
                LogLevel.WARN -> Log.w("Switchboard", message, throwable)
                LogLevel.ERROR -> Log.e("Switchboard", message, throwable)
            }
        } else {
            when (level) {
                LogLevel.DEBUG -> Log.d("Switchboard", message)
                LogLevel.INFO -> Log.i("Switchboard", message)
                LogLevel.WARN -> Log.w("Switchboard", message)
                LogLevel.ERROR -> Log.e("Switchboard", message)
            }
        }
    }
}
