package services.pixelpulse.switchboard.android

import android.content.Context
import services.pixelpulse.switchboard.core.OverrideStorage
import services.pixelpulse.switchboard.core.SwitchboardContext

/**
 * Concrete Android application context wrapper bridging runtime evaluations with native override storage layers.
 *
 * Enforces strict isolation by securely accessing and retaining `applicationContext` only, guaranteeing zero
 * retention of dynamic `Activity` or `Fragment` lifecycle boundaries to prevent memory leaks.
 */
public class AndroidSwitchboardContext internal constructor(
    private val overrideStorage: OverrideStorage
) : SwitchboardContext {

    /**
     * Initializes the context capability container derived from the application scope.
     *
     * @param context The incoming native Android context from which the application context is derived.
     */
    public constructor(context: Context) : this(
        overrideStorage = DataStoreOverrideStorage(context.applicationContext ?: context)
    )

    override fun overrideStorage(): OverrideStorage = overrideStorage
}
