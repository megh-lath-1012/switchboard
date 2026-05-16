package dev.meghlath.switchboard.annotations

/**
 * Declares a Long feature flag.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class LongFlag(
    public val default: Long,
    public val description: String = "",
    public val category: String = ""
)
