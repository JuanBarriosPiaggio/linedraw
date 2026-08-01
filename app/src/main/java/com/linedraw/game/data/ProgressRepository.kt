package com.linedraw.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linedraw")

data class Settings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val adsRemoved: Boolean = false,
)

/** All locally persisted state: unlocked progress, stars, settings and ad counters. */
class ProgressRepository(private val context: Context) {

    private object Keys {
        val UNLOCKED_LEVEL = intPreferencesKey("unlocked_level")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val TOTAL_COMPLETIONS = intPreferencesKey("total_completions")
        val COMPLETIONS_SINCE_INTERSTITIAL = intPreferencesKey("completions_since_interstitial")

        fun stars(levelId: Int) = intPreferencesKey("stars_$levelId")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            soundEnabled = prefs[Keys.SOUND] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            adsRemoved = prefs[Keys.ADS_REMOVED] ?: false,
        )
    }

    /** Highest level id the player is allowed to play (1-based, defaults to 1). */
    val unlockedLevel: Flow<Int> = context.dataStore.data.map { it[Keys.UNLOCKED_LEVEL] ?: 1 }

    val totalCompletions: Flow<Int> =
        context.dataStore.data.map { it[Keys.TOTAL_COMPLETIONS] ?: 0 }

    fun starsForLevel(levelId: Int): Flow<Int> =
        context.dataStore.data.map { it[Keys.stars(levelId)] ?: 0 }

    fun starsMap(levelIds: List<Int>): Flow<Map<Int, Int>> = context.dataStore.data.map { prefs ->
        levelIds.associateWith { prefs[Keys.stars(it)] ?: 0 }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        context.dataStore.edit { it[Keys.ADS_REMOVED] = removed }
    }

    /**
     * Records a completed level: stars (best kept), unlock of the next level and
     * the interstitial counters.
     *
     * @return true if the caller should show an interstitial ad now.
     */
    suspend fun recordCompletion(levelId: Int, stars: Int, totalLevels: Int): Boolean {
        var showInterstitial = false
        context.dataStore.edit { prefs ->
            val bestStars = maxOf(prefs[Keys.stars(levelId)] ?: 0, stars)
            prefs[Keys.stars(levelId)] = bestStars

            val unlocked = prefs[Keys.UNLOCKED_LEVEL] ?: 1
            if (levelId >= unlocked && levelId < totalLevels) {
                prefs[Keys.UNLOCKED_LEVEL] = levelId + 1
            }

            val total = (prefs[Keys.TOTAL_COMPLETIONS] ?: 0) + 1
            prefs[Keys.TOTAL_COMPLETIONS] = total

            val since = (prefs[Keys.COMPLETIONS_SINCE_INTERSTITIAL] ?: 0) + 1
            // Grace period: never interrupt the first 3 completions, then every 4th.
            if (total > 3 && since >= 4) {
                showInterstitial = true
                prefs[Keys.COMPLETIONS_SINCE_INTERSTITIAL] = 0
            } else {
                prefs[Keys.COMPLETIONS_SINCE_INTERSTITIAL] = since
            }
        }
        return showInterstitial
    }

    suspend fun currentSettings(): Settings = settings.first()

    /**
     * Wipes all gameplay progress (unlocks, stars, ad counters) while keeping
     * user preferences and the Remove Ads purchase intact.
     */
    suspend fun resetAllProgress() {
        context.dataStore.edit { prefs ->
            val keep = mapOf<Preferences.Key<Boolean>, Boolean?>(
                Keys.SOUND to prefs[Keys.SOUND],
                Keys.HAPTICS to prefs[Keys.HAPTICS],
                Keys.ADS_REMOVED to prefs[Keys.ADS_REMOVED],
            )
            prefs.clear()
            keep.forEach { (key, value) -> if (value != null) prefs[key] = value }
        }
    }
}
