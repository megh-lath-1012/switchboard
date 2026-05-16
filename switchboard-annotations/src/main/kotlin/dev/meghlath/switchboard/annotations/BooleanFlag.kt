package dev.meghlath.switchboard.annotations

/**
 * Declares a Boolean feature flag.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class BooleanFlag(
    public val default: Boolean,
    public val description: String = "",
    public val category: String = ""
)
