package dev.meghlath.switchboard.annotations

/**
 * Indicates that [Switchboard.init] was invoked without providing a populated feature flag registry.
 *
 * Forgetting to pass the KSP-generated registry (e.g., `SwitchboardRegistryImpl`) results in an empty
 * state screen within the Switchboard debug UI dashboard.
 *
 * If this omission is intentional (e.g., in a test environment or specialized app flavor), this warning
 * can be safely suppressed.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Switchboard.init() invoked without a feature flag registry. The Compose Debug UI will show an empty state screen."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
public annotation class SwitchboardRegistryOmitted
