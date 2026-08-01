package com.linedraw.game

import android.app.Application
import com.linedraw.game.ads.AdManager
import com.linedraw.game.ads.AdMobAdManager
import com.linedraw.game.ads.MockAdManager
import com.linedraw.game.audio.FeedbackManager
import com.linedraw.game.billing.BillingManager
import com.linedraw.game.data.LevelRepository
import com.linedraw.game.data.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application-scoped service locator: repositories, ads, billing and feedback. */
class LineDrawApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var levelRepository: LevelRepository
        private set
    lateinit var progressRepository: ProgressRepository
        private set
    lateinit var adManager: AdManager
        private set
    lateinit var billingManager: BillingManager
        private set
    lateinit var feedback: FeedbackManager
        private set

    override fun onCreate() {
        super.onCreate()

        levelRepository = LevelRepository(this)
        progressRepository = ProgressRepository(this)
        feedback = FeedbackManager(this)

        adManager = if (BuildConfig.USE_MOCK_ADS) MockAdManager() else AdMobAdManager(this)
        adManager.initialize()

        billingManager = BillingManager(this) { owned ->
            progressRepository.setAdsRemoved(owned)
        }
        billingManager.connect()

        // Keep feedback toggles in sync with persisted settings.
        applicationScope.launch {
            progressRepository.settings.collect { settings ->
                feedback.soundEnabled = settings.soundEnabled
                feedback.hapticsEnabled = settings.hapticsEnabled
            }
        }
    }
}
