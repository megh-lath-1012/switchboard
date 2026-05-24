# switchboard-compose
Provides a Jetpack Compose debug screen for inspecting and overriding Switchboard feature flags at runtime.

## PREREQUISITES
- `switchboard-core` must be installed.
- `switchboard-android` must be installed and initialized.
- Jetpack Compose must be enabled in your project.
- Minimum SDK: 23

## INSTALLATION
```kotlin
implementation("services.pixelpulse:switchboard-compose:$switchboardVersion")
```
To add the debug screen to a Jetpack Navigation NavGraph:
```kotlin
composable("switchboard_debug") {
    val stateHolder = remember { SwitchboardDebugStateHolder(SwitchboardDebugAccessor) }
    SwitchboardDebugScreen(stateHolder = stateHolder)
}
```

## QUICK START
```kotlin
import services.pixelpulse.switchboard.compose.SwitchboardDebugScreen
import services.pixelpulse.switchboard.compose.SwitchboardDebugStateHolder
import services.pixelpulse.switchboard.core.SwitchboardDebugAccessor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun MyDebugMenu() {
    val stateHolder = remember { SwitchboardDebugStateHolder(SwitchboardDebugAccessor) }
    SwitchboardDebugScreen(stateHolder = stateHolder)
}
```

## CONFIGURATION OPTIONS
- **`stateHolder`**
  - Type: `SwitchboardDebugStateHolder`
  - Default: None (Required)
  - What it does: Controller encapsulating the immutable state streams and event mutators that power the screen.
- **`modifier`**
  - Type: `Modifier`
  - Default: `Modifier`
  - What it does: Custom styling applied to the top-level host layout.

*Note on behaviors:* The screen natively supports text filtering via a search bar spanning keys and descriptions. A "Clear Overrides" button appears automatically when any override is active. If the registry is empty, it displays a detailed empty state indicating that no flags were discovered.

## HOW IT WORKS
The debug screen aggregates feature flag states into a clear 3-column view, visually separating values sourced from local overrides, remote backends, and default definitions. It utilizes a `SwitchboardDebugStateHolder` pattern rather than a traditional ViewModel to manage state, ensuring a decoupled architecture that does not rely on Android's ViewModel lifecycle. 

State updates are exposed as `@Stable` and `@Immutable` UI models collected from a StateFlow. When a user modifies a value, the state holder intercepts the input, validates it against the flag's native type safety constraints, and writes the override back to DataStore via the `SwitchboardDebugAccessor`. 

The screen dynamically renders specific input interfaces (e.g., switches for booleans, dropdowns for enums) adhering to the flag type. It continuously observes changes, meaning any background syncs from a remote backend will instantly update the UI without manual intervention.

## RELEASE BUILD BEHAVIOR
When `debugEnabled = false` (or in a minified release build), the `setOverride` function in the accessor becomes a no-op, and the `overrideChanges()` flow returns `emptyFlow()`. The debug screen can still be mounted without crashing, but it will show no override controls and will not allow any modifications.

## COMMON ISSUES
**Problem:** Flags not appearing / showing Empty State.
**Cause:** `Switchboard.init()` was invoked without a populated feature flag registry.
**Fix:** Ensure you pass the KSP-generated implementation (e.g., `SwitchboardRegistryImpl`) during initialization.

**Problem:** Overrides not persisting when set.
**Cause:** Application is running in a configuration where `debugEnabled = false`.
**Fix:** Verify that initialization uses `BuildConfig.DEBUG` or another explicit debug flag.

**Problem:** UI does not update on remote fetch.
**Cause:** The backend flow is not emitting changes or backend is misconfigured.
**Fix:** Ensure the remote config provider properly implements the `changes()` flow.

## RELATED MODULES
| Module | Purpose |
|--------|---------|
| [switchboard-compose](../switchboard-compose/README.md) | Jetpack Compose UI for runtime flag overrides. |
| [switchboard-shake](../switchboard-shake/README.md) | Shake-to-reveal gesture for the debug interface. |
| [switchboard-firebase](../switchboard-firebase/README.md) | Remote Config backend provider for Switchboard. |
| [switchboard-okhttp](../switchboard-okhttp/README.md) | Network interceptor injecting flag state into logs. |

## LICENSE
Apache 2.0. See the root [LICENSE](../LICENSE) file for details.
