# switchboard-shake
Provides a global shake gesture monitor that triggers the Switchboard debug interface on demand.

## PREREQUISITES
- `switchboard-core` must be installed.
- `switchboard-android` must be installed and initialized.
- Minimum SDK: 23

## INSTALLATION
```kotlin
implementation("services.pixelpulse:switchboard-shake:$switchboardVersion")
```
Initialize it inside your custom `Application` class:
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            SwitchboardShakeDetector.install(this)
        }
    }
}
```

## QUICK START
```kotlin
import services.pixelpulse.switchboard.shake.SwitchboardShakeDetector
import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Installs the detector with default 3 shakes over 1000ms
        SwitchboardShakeDetector.install(this)
    }
}
```

## CONFIGURATION OPTIONS
- **`application`**
  - Type: `Application`
  - Default: None (Required)
  - What it does: The hosting application instance used to register lifecycle callbacks.
- **`shakeCount`**
  - Type: `Int`
  - Default: `3`
  - What it does: Minimum number of shakes required to trigger the debug UI.
- **`windowMs`**
  - Type: `Long`
  - Default: `1000L`
  - What it does: Time window in milliseconds to accumulate the required `shakeCount`.

*Annotation Usage:* Add the `@SwitchboardIgnoreShake` annotation to any `Activity` class where the shake detector should not operate.

## HOW IT WORKS
The module uses an `ActivityLifecycleCallbacks` pattern to automatically manage the device accelerometer sensor. It only registers the `SensorEventListener` when an activity is resumed and strictly unregisters it when paused. This ensures zero battery impact when the application is backgrounded.

Before attaching to a resumed activity, the detector checks if the activity class is annotated with `@SwitchboardIgnoreShake`. If present, it skips sensor registration entirely, preventing accidental triggers on sensitive or high-motion screens (e.g., a camera view or a game).

When the requisite threshold of motion is accumulated within the specified time window, the detector launches `SwitchboardDebugActivity`. This activity acts as a transparent overlay housing the Jetpack Compose debug screen on top of the current task stack, preventing disruption of the underlying application flow.

## RELEASE BUILD BEHAVIOR
It is highly recommended to call the `install()` function inside an `if (BuildConfig.DEBUG)` block. If called in a release build, the module will not crash, but it will register unnecessary sensor listeners and consume battery monitoring for shakes that ultimately launch a no-op debug activity.

## COMMON ISSUES
**Problem:** Shake gesture not triggering the debug UI.
**Cause:** The detector was installed using an `Activity` context instead of the `Application` context, or the app is running in an emulator without emulated motion.
**Fix:** Ensure `SwitchboardShakeDetector.install(this)` is called from your `Application` subclass.

**Problem:** Debug activity not dismissed on back press.
**Cause:** The transparent overlay might intercept touches incorrectly if the theme is overridden globally.
**Fix:** Ensure the base application theme does not forcefully override transparent window backgrounds.

**Problem:** Triggering on sensitive screens (e.g. camera).
**Cause:** Missing exclusion annotation.
**Fix:** Add the `@SwitchboardIgnoreShake` annotation to the sensitive `Activity` class.

## RELATED MODULES
| Module | Purpose |
|--------|---------|
| [switchboard-compose](../switchboard-compose/README.md) | Jetpack Compose UI for runtime flag overrides. |
| [switchboard-shake](../switchboard-shake/README.md) | Shake-to-reveal gesture for the debug interface. |
| [switchboard-firebase](../switchboard-firebase/README.md) | Remote Config backend provider for Switchboard. |
| [switchboard-okhttp](../switchboard-okhttp/README.md) | Network interceptor injecting flag state into logs. |

## LICENSE
Apache 2.0. See the root [LICENSE](../LICENSE) file for details.
