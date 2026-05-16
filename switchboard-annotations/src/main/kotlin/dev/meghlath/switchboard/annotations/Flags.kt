package dev.meghlath.switchboard.annotations

/**
 * Marks an object or class as a container for Switchboard feature flags.
 *
 * Properties inside a container annotated with [Flags] must be annotated with one of the 
 * typed flag annotations (e.g., [BooleanFlag], [IntFlag], [EnumFlag]) to be discovered 
 * and processed by the Switchboard KSP processor.
 *
 * The KSP processor generates typed accessor objects and registers all discovered flags
 * into a centralized registry.
 *
 * **Usage Example:**
 * ```kotlin
 * @Flags
 * public object AppFlags {
 *     @BooleanFlag(default = false, description = "Enable new home screen layout", category = "UI")
 *     public val enableNewHome: Boolean = false
 *     
 *     @EnumFlag(default = "CONTROL", enumClass = CheckoutVariant::class)
 *     public val checkoutVariant: CheckoutVariant = CheckoutVariant.CONTROL
 * }
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class Flags
