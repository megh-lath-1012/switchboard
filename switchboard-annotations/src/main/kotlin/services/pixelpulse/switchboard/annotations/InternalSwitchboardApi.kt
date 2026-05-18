package services.pixelpulse.switchboard.annotations

/**
 * Marks an API as internal to the Switchboard library infrastructure.
 *
 * Such APIs are strictly designed to bridge functionality between decoupled core layers
 * (e.g., exposing raw backend or override stream accesses to the debug UI module) and should
 * **never** be invoked directly from consumer production code.
 *
 * Using these APIs bypasses core synchronization guarantees and may lead to binary incompatibilities
 * in future releases.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal to Switchboard infrastructure and must not be used in external code."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalSwitchboardApi
