package services.pixelpulse.switchboard.annotations

import kotlin.reflect.KClass

/**
 * Declares an Enum feature flag.
 *
 * @param default The name of the enum entry (e.g., "CONTROL").
 * @param enumClass The [KClass] of the enum.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
public annotation class EnumFlag(
    public val default: String,
    public val enumClass: KClass<out Enum<*>>,
    public val description: String = "",
    public val category: String = ""
)
