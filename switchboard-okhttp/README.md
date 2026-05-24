# switchboard-okhttp
Provides an OkHttp interceptor that automatically appends Switchboard feature flag resolution states to outgoing network request logs.

## PREREQUISITES
- `switchboard-core` must be installed.
- `switchboard-android` must be installed and initialized.
- OkHttp 3.x or 4.x must be used for networking.
- Minimum SDK: 23

## INSTALLATION
```kotlin
implementation("services.pixelpulse:switchboard-okhttp:$switchboardVersion")
```
Add the interceptor to your `OkHttpClient.Builder`:
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(SwitchboardInterceptor())
    .build()
```

## QUICK START
```kotlin
import services.pixelpulse.switchboard.okhttp.SwitchboardInterceptor
import services.pixelpulse.switchboard.okhttp.SwitchboardLogLevel
import okhttp3.OkHttpClient

val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(SwitchboardInterceptor(logLevel = SwitchboardLogLevel.OVERRIDES_ONLY))
    .build()
```

## CONFIGURATION OPTIONS
- **`logLevel`**
  - Type: `SwitchboardLogLevel`
  - Default: `SwitchboardLogLevel.OVERRIDES_ONLY`
  - What it does: Controls the granularity of the network logs.
    - `ALL`: Logs the current resolved state for every registered feature flag.
    - `OVERRIDES_ONLY`: Only logs flags that currently have a local debug override active.

## HOW IT WORKS
The interceptor taps into the OkHttp chain to inject feature flag context alongside your network traffic. Before an outgoing request proceeds, it iterates through the active registry using `SwitchboardDebugAccessor` to evaluate the resolution hierarchy of each flag (Override > Remote > Default).

By default, it runs in `OVERRIDES_ONLY` mode. This is specifically designed to reduce log noise while highlighting critical debugging context. If a developer forgets they left an active override enabled from a previous testing session, the interceptor appends a prominent warning glyph (`OVERRIDE ⚠️`) to the network log. This makes it immediately obvious why an API request might be behaving unexpectedly or receiving alternative payloads.

The structured log block is routed through `Switchboard.logHandler` at the `DEBUG` level, ensuring it integrates cleanly with your existing logging pipeline (e.g., Timber or standard Logcat) instead of forcing a specific console output.

## RELEASE BUILD BEHAVIOR
When `debugEnabled = false` (standard for release builds), the interceptor immediately short-circuits and acts as a pure pass-through interceptor. It performs zero registry lookups, formats zero strings, and produces zero overhead, making it perfectly safe to leave permanently installed in your production OkHttp client.

## COMMON ISSUES
**Problem:** No feature flag logs appearing in Logcat.
**Cause:** `logLevel` is set to `OVERRIDES_ONLY` and no local overrides are currently active, or a custom `LogHandler` is discarding `DEBUG` level logs.
**Fix:** Change the initialization to `SwitchboardInterceptor(logLevel = SwitchboardLogLevel.ALL)` or activate an override.

**Problem:** Logs are unexpectedly appearing in release builds.
**Cause:** Switchboard was initialized with `debugEnabled = true` in a production environment.
**Fix:** Ensure `Switchboard.init()` dynamically passes `BuildConfig.DEBUG` as the debug flag.

**Problem:** Flag names not appearing in logs.
**Cause:** The generated `SwitchboardRegistryImpl` was not provided to `Switchboard.init()`.
**Fix:** Pass the correct registry implementation during app startup.

## RELATED MODULES
| Module | Purpose |
|--------|---------|
| [switchboard-compose](../switchboard-compose/README.md) | Jetpack Compose UI for runtime flag overrides. |
| [switchboard-shake](../switchboard-shake/README.md) | Shake-to-reveal gesture for the debug interface. |
| [switchboard-firebase](../switchboard-firebase/README.md) | Remote Config backend provider for Switchboard. |
| [switchboard-okhttp](../switchboard-okhttp/README.md) | Network interceptor injecting flag state into logs. |

## LICENSE
Apache 2.0. See the root [LICENSE](../LICENSE) file for details.
