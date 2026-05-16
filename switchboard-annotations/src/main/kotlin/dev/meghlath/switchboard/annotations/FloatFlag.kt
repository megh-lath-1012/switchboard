package dev.meghlath.switchboard.annotations

/**
 * Declares a Float feature flag.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class FloatFlag(
    public val default: Float,
    public val description: String = "",
    public val category: String = ""
)
