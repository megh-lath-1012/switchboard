# switchboard-firebase
Provides a Firebase Remote Config backend integration for resolving Switchboard feature flags remotely.

## PREREQUISITES
- `switchboard-core` must be installed.
- `switchboard-android` must be installed and initialized.
- Firebase must be configured in your project.
- Minimum SDK: 23

## INSTALLATION
```kotlin
implementation("services.pixelpulse:switchboard-firebase:$switchboardVersion")
```
Wire the backend during Switchboard initialization:
```kotlin
Switchboard.init(
    registry = SwitchboardRegistryImpl(),
    backend = FirebaseRemoteConfigBackend()
)
```

## QUICK START
```kotlin
import services.pixelpulse.switchboard.firebase.FirebaseRemoteConfigBackend
import services.pixelpulse.switchboard.core.Switchboard

fun initializeRemoteFlags() {
    Switchboard.init(
        registry = SwitchboardRegistryImpl(), // Pass your generated registry
        backend = FirebaseRemoteConfigBackend()
    )
}
```

## CONFIGURATION OPTIONS
- **`remoteConfig`**
  - Type: `FirebaseRemoteConfig`
  - Default: `FirebaseRemoteConfig.getInstance()`
  - What it does: The Firebase instance used to fetch and listen for remote configuration changes. Allows injecting a custom instance for testing or specific Firebase apps.

## HOW IT WORKS
This backend acts as a bridge between Switchboard's strongly-typed flag resolution and Firebase Remote Config's underlying storage. Switchboard requests values from Firebase as standard Strings, which the backend then attempts to coerce into the specific native type defined by the flag (Boolean, Int, Long, Float, Double, or String).

If a type mismatch occurs during coercion (e.g., parsing "true" as an Int), the backend logs a warning via `Switchboard.logHandler` and explicitly returns `null`. This intentional null-on-failure behavior causes Switchboard's resolution engine to safely fall back to the flag's local default value rather than crashing or providing invalid state.

Real-time updates are powered by wiring Firebase's `addOnConfigUpdateListener` into a Kotlin `callbackFlow`. When Firebase pushes a configuration update, the listener emits to the flow, notifying Switchboard that remote values have shifted and triggering downstream state updates across the application.

## RELEASE BUILD BEHAVIOR
There is no special release behavior for this module. The backend functions identically in both debug and release environments, reliably fetching and coercing values from Firebase Remote Config without any developer intervention.

## COMMON ISSUES
**Problem:** All values are resolving as default.
**Cause:** Firebase configuration has not been fetched or activated yet, or requests are being throttled by Firebase.
**Fix:** Ensure you are calling `FirebaseRemoteConfig.getInstance().fetchAndActivate()` early in your app lifecycle.

**Problem:** Type coercion warning appears in Logcat.
**Cause:** The value defined in the Firebase Console has the wrong type or is malformed for the requested flag.
**Fix:** Verify the parameter in the Firebase Console matches the type defined in your `@SwitchboardFlag` definition.

**Problem:** The `changes()` Flow is not emitting on updates.
**Cause:** Missing Google Play Services on the device, or using an outdated Firebase SDK version that does not support `addOnConfigUpdateListener`.
**Fix:** Verify Google Play Services is available and update your Firebase dependencies.

## RELATED MODULES
| Module | Purpose |
|--------|---------|
| [switchboard-compose](../switchboard-compose/README.md) | Jetpack Compose UI for runtime flag overrides. |
| [switchboard-shake](../switchboard-shake/README.md) | Shake-to-reveal gesture for the debug interface. |
| [switchboard-firebase](../switchboard-firebase/README.md) | Remote Config backend provider for Switchboard. |
| [switchboard-okhttp](../switchboard-okhttp/README.md) | Network interceptor injecting flag state into logs. |

## LICENSE
Apache 2.0. See the root [LICENSE](../LICENSE) file for details.
