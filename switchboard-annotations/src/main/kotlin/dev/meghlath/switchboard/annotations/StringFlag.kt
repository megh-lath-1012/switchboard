package dev.meghlath.switchboard.annotations

/**
 * Declares a String feature flag.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class StringFlag(
    public val default: String,
    public val description: String = "",
    public val category: String = ""
)
