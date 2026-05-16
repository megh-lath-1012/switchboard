package dev.meghlath.switchboard.sample

import dev.meghlath.switchboard.annotations.BooleanFlag
import dev.meghlath.switchboard.annotations.DoubleFlag
import dev.meghlath.switchboard.annotations.EnumFlag
import dev.meghlath.switchboard.annotations.Flags
import dev.meghlath.switchboard.annotations.FloatFlag
import dev.meghlath.switchboard.annotations.IntFlag
import dev.meghlath.switchboard.annotations.LongFlag
import dev.meghlath.switchboard.annotations.StringFlag

enum class CheckoutVariant { CONTROL, VARIANT_A, VARIANT_B }

@Flags
object AppFlags {
    @BooleanFlag(
        default = false, 
        description = "Skip the shipping step in checkout",
        category = "Checkout"
    )
    val skipShippingStep: Boolean = false
    
    @IntFlag(
        default = 5000, 
        description = "API timeout in milliseconds",
        category = "Network"
    )
    val apiTimeoutMs: Int = 5000
    
    @LongFlag(
        default = 100_000L, 
        description = "Max cart value in cents",
        category = "Checkout"
    )
    val maxCartValueCents: Long = 100_000L
    
    @FloatFlag(
        default = 0.0f, 
        description = "Discount percentage to auto-apply",
        category = "Pricing"
    )
    val autoDiscountPercent: Float = 0.0f
    
    @DoubleFlag(
        default = 1.0, 
        description = "Currency conversion multiplier",
        category = "Pricing"
    )
    val currencyMultiplier: Double = 1.0
    
    @StringFlag(
        default = "Welcome", 
        description = "Greeting text on home screen",
        category = "UI"
    )
    val greetingText: String = "Welcome"
    
    @EnumFlag(
        default = "CONTROL",
        enumClass = CheckoutVariant::class,
        description = "Checkout flow A/B variant",
        category = "Experiments"
    )
    val checkoutVariant: CheckoutVariant = CheckoutVariant.CONTROL
}
