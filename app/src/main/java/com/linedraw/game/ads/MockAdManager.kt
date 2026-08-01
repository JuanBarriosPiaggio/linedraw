package com.linedraw.game.ads

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * No-network stand-in used in debug builds: interstitials "show" instantly,
 * rewarded ads always grant the reward. Lets the full game loop (including
 * hint gating) be exercised in an emulator with zero AdMob setup.
 */
class MockAdManager : AdManager {

    private val _rewardedReady = MutableStateFlow(true)
    override val rewardedReady: StateFlow<Boolean> = _rewardedReady

    override fun initialize() {
        Log.d(TAG, "Mock ads initialized")
    }

    override fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        Log.d(TAG, "Mock interstitial shown")
        onDismissed()
    }

    override fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        Log.d(TAG, "Mock rewarded ad shown — granting reward")
        onReward()
    }

    private companion object {
        const val TAG = "MockAdManager"
    }
}
