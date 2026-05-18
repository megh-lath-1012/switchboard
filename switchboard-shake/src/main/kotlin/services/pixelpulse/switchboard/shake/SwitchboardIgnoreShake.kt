package services.pixelpulse.switchboard.shake

/**
 * Opt-out marker for activities that should not trigger the Switchboard debug UI
 * even when a valid shake gesture is detected.
 *
 * Useful for splash screens, secure login flows, or critical checkout paths
 * where unexpected UI overlays are prohibited.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class SwitchboardIgnoreShake
