package dev.meghlath.switchboard.core

/**
 * Context container interface integrating the target platform's execution capabilities
 * into the pure-Kotlin Switchboard core runtime engine.
 *
 * Designed to guarantee complete KMP (Kotlin Multiplatform) isolation without passing `Any`
 * or relying on JVM/Android-specific filesystem classes.
 *
 * **Usage Example:**
 * ```kotlin
 * val overrideStorage = context.overrideStorage()
 * ```
 */
public interface SwitchboardContext {
    /**
     * Provides the initialized platform storage implementation for feature flag overrides.
     *
     * @return The platform-specific [OverrideStorage] engine.
     */
    public fun overrideStorage(): OverrideStorage
}
