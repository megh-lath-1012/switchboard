package dev.meghlath.switchboard.annotations

/**
 * Declares a Double feature flag.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class DoubleFlag(
    public val default: Double,
    public val description: String = "",
    public val category: String = ""
)
