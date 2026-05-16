package dev.meghlath.switchboard.android

import android.content.Context
import dev.meghlath.switchboard.annotations.SwitchboardRegistryOmitted
import dev.meghlath.switchboard.core.Backend
import dev.meghlath.switchboard.core.EmptySwitchboardRegistry
import dev.meghlath.switchboard.core.NoOpBackend
import dev.meghlath.switchboard.core.Switchboard
import dev.meghlath.switchboard.core.SwitchboardRegistry

/**
 * Initializes the Switchboard runtime engine for Android applications without providing an explicit flag registry.
 *
 * **Warning:** Invoking this overload omits global flag metadata registration, causing the Compose Debug UI
 * dashboard to render an empty state screen.
 *
 * @param context The application or activity context.
 * @param backend Configuration provider source. Defaults to [NoOpBackend].
 * @param debugEnabled Whether local overrides are evaluated. Defaults to `false`.
 */
@SwitchboardRegistryOmitted
public fun Switchboard.init(
    context: Context,
    backend: Backend = NoOpBackend,
    debugEnabled: Boolean = false
) {
    init(
        context = context,
        registry = EmptySwitchboardRegistry,
        backend = backend,
        debugEnabled = debugEnabled
    )
}

/**
 * Initializes the Switchboard runtime engine for Android applications with a centralized feature flag registry.
 *
 * Automatically registers the native [AndroidLogger] functional router to output diagnostics to `android.util.Log`
 * and provisions a leak-free [AndroidSwitchboardContext] capable of resolving overrides via Jetpack DataStore Preferences.
 *
 * **Usage Example:**
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Switchboard.init(
 *             context = this,
 *             registry = SwitchboardRegistryImpl,
 *             backend = NoOpBackend,
 *             debugEnabled = BuildConfig.DEBUG
 *         )
 *     }
 * }
 * ```
 *
 * @param context The application or activity context from which the isolated applicationContext capability is extracted.
 * @param registry Centralized catalog containing metadata for all feature flags discovered by KSP.
 * @param backend Configuration provider source. Defaults to [NoOpBackend].
 * @param debugEnabled Whether local overrides are evaluated. Defaults to `false`.
 */
public fun Switchboard.init(
    context: Context,
    registry: SwitchboardRegistry,
    backend: Backend = NoOpBackend,
    debugEnabled: Boolean = false
) {
    setLogHandler(AndroidLogger)
    init(
        context = AndroidSwitchboardContext(context),
        registry = registry,
        backend = backend,
        debugEnabled = debugEnabled
    )
}
