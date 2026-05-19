<p align="center">
  <img src="assets/banner.png" alt="Switchboard Banner" width="100%">
</p>

# Switchboard 🎛️

[![CI Status](https://github.com/megh-lath-1012/switchboard/actions/workflows/ci.yml/badge.svg)](https://github.com/megh-lath-1012/switchboard/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/services.pixelpulse/switchboard-core.svg)](https://central.sonatype.com/artifact/services.pixelpulse/switchboard-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![KSP](https://img.shields.io/badge/KSP-2.0.21--1.0.27-orange.svg)](https://github.com/google/ksp)

**Switchboard is a compile-time type-safe feature flagging SDK for Android that automatically generates clean Kotlin extension properties from annotated declarations.** It bridges local defaults, remote configurations, and local debug override menus with zero runtime casting, zero duplicate fallback declarations, and zero hardcoded string keys.

---

## 🚀 Key Features

* **🛡️ Compile-Time Type Safety**: Declare flags once as Kotlin properties. KSP generates extension accessors for native types (`Boolean`, `Int`, `Long`, `Float`, `Double`, `String`, `Enum`) with verified compiler support.
* **🎛️ Zero-Reflective Performance**: Avoids heavy runtime reflection by resolving code structures at compile-time, ensuring instant flag resolution without slowing down your app's main thread.
* **🤝 Pluggable Architecture**: Map your configurations seamlessly to any remote backend (like Firebase Remote Config or custom HTTP providers) with flow-based reactive updates.
* **📊 Auto-Generated Debug Overrides**: Generates a local override registry supporting an optional Jetpack Compose debug sheet and shake-to-open gesture for testing flag combinations during QA.

---

## 🔍 Before vs. After

### Standard Android Approach ❌
Setting up and retrieving feature flags manually is verbose, requires duplicating fallback defaults, and relies on error-prone string keys:

```kotlin
// 1. Hardcoded string keys
object ConfigKeys {
    const val API_TIMEOUT = "api_timeout_ms"
    const val EXPERIMENT_VARIANT = "checkout_variant"
}

// 2. Retrieval involves try-catch blocks and manual type fallback mapping
val timeoutMs = try {
    remoteConfig.getLong(ConfigKeys.API_TIMEOUT).toInt()
} catch (e: Exception) {
    5000 // Duplicated fallback value
}

val rawVariant = remoteConfig.getString(ConfigKeys.EXPERIMENT_VARIANT)
val variant = try { 
    CheckoutVariant.valueOf(rawVariant) 
} catch (e: Exception) { 
    CheckoutVariant.CONTROL // Manual parsing fallback
}
```

### With Switchboard ✅
Flags are declared once in a clean container, and KSP automatically makes them available as type-safe extension properties on `Switchboard`:

```kotlin
@Flags
object AppFlags {
    @IntFlag(default = 5000, category = "Network")
    val apiTimeoutMs: Int = 5000

    @EnumFlag(default = "CONTROL", enumClass = CheckoutVariant::class, category = "Checkout")
    val checkoutVariant: CheckoutVariant = CheckoutVariant.CONTROL
}

// Access flag values dynamically with zero boilerplate:
val timeoutMs = Switchboard.apiTimeoutMs
val variant = Switchboard.checkoutVariant
```

---

## 🛠️ Quick Start & Installation

### 1. Add Dependencies

Add the dependencies to your `libs.versions.toml`:

```toml
[versions]
switchboard = "0.1.0"

[libraries]
switchboard-core = { module = "services.pixelpulse:switchboard-core", version.ref = "switchboard" }
switchboard-android = { module = "services.pixelpulse:switchboard-android", version.ref = "switchboard" }
switchboard-ksp = { module = "services.pixelpulse:switchboard-ksp", version.ref = "switchboard" }
# Optional modules
switchboard-compose = { module = "services.pixelpulse:switchboard-compose", version.ref = "switchboard" }
switchboard-shake = { module = "services.pixelpulse:switchboard-shake", version.ref = "switchboard" }
```

Apply the KSP plugin and add the dependencies to your app-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.switchboard.android)
    ksp(libs.switchboard.ksp)
    
    // Optional debug modules
    implementation(libs.switchboard.compose)
    implementation(libs.switchboard.shake)
}
```

---

## 📖 Minimal Usage Example

Initialize `Switchboard` inside your `Application` class, pointing to the KSP-generated `SwitchboardRegistryImpl`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Switchboard.init(
            context = this,
            registry = SwitchboardRegistryImpl, // Generated automatically by KSP
            backend = FirebaseRemoteConfigBackend(), // Optional: Map to remote source
            debugEnabled = BuildConfig.DEBUG
        )

        // Install shake-to-open gesture for debug builds
        if (BuildConfig.DEBUG) {
            SwitchboardShakeDetector.install(this)
        }
    }
}
```

---

## 📄 License

```text
Copyright 2026 Switchboard Open Source Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
