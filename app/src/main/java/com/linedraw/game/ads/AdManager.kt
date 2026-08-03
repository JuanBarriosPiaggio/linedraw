package com.linedraw.game.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over all full-screen ad calls so gameplay can be developed and
 * tested without live AdMob credentials (see [MockAdManager]).
 *
 * Banner ads are handled by the AdBanner composable, which also respects
 * the same mock/real switch.
 */
interface AdManager {

    /** True when a rewarded ad is loaded and ready to show. */
    val rewardedReady: StateFlow<Boolean>

    fun initialize()

    /**
     * Shows an interstitial if one is loaded. Always invokes [onDismissed]
     * (immediately when nothing is ready).
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit)

    /**
     * Shows a rewarded ad. [onReward] fires only when the user earned the reward.
     * [onUnavailable] fires when no ad could be shown.
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit)
}

object AdConfig {
    /** Production AdMob unit IDs for Line Draw (Vexlo). Debug builds still use MockAdManager. */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-5881206053165150/2095368182"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5881206053165150/5322618597"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-5881206053165150/5651469810"
}
