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
    /**
     * Google's PUBLIC TEST ad unit IDs — safe to keep during development.
     * TODO: Replace with your real AdMob ad unit IDs before release (see README).
     */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
}
