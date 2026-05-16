package dev.meghlath.switchboard.annotations

/**
 * Declares an Integer feature flag.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class IntFlag(
    public val default: Int,
    public val description: String = "",
    public val category: String = ""
)
