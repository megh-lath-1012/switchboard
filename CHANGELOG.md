# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-18

### Changed
- **Group ID / package rename**: Initial public release is published under group ID `services.pixelpulse` (Maven coordinates `services.pixelpulse:switchboard-*`). The internal development group ID `dev.meghlath` was never published and is replaced in full.

---

## [0.1.0] - 2026-05-15

### Added
- **Core Engine (`switchboard-core`)**: Introduced a thread-safe, fast resolution engine for feature flags.
- **KSP Code Generator (`switchboard-ksp`)**: Automatic type-safe flag registry generation and property binding interceptor. Supported types: Boolean, Int, Long, Float, Double, String, Enum.
- **Compose UI (`switchboard-compose`)**: Shipped the `SwitchboardDebugScreen` providing real-time oversight of all resolved feature flags across the app.
- **Shake Detector (`switchboard-shake`)**: Quick-access mechanism to invoke the Debug UI on physical devices or emulators.
- **Firebase Remote Config Backend (`switchboard-firebase`)**: Built-in reactive flow syncing directly to Firebase endpoints.
- **OkHttp Interceptor (`switchboard-okhttp`)**: Included flag attributions in network traffic logs, bridging the gap between networking diagnostics and UI state.
- **Android Context Binder (`switchboard-android`)**: Leak-proof `DataStore` driven bindings for robust override persistence.
- **Sample Demonstration (`sample`)**: A functional checkout-flow app demonstrating the library working in tandem with OkHttp, Firebase mock, and Compose Navigation.
