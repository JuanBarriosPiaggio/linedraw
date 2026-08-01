package com.linedraw.game.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Loads the bundled level pack from assets/levels.json (cached after first load). */
class LevelRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: List<Level>? = null

    suspend fun loadLevels(): List<Level> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("levels.json").bufferedReader().use { it.readText() }
            val pack = json.decodeFromString<LevelPack>(text)
            pack.levels.sortedBy { it.id }.also { cache = it }
        }
    }

    suspend fun getLevel(id: Int): Level? = loadLevels().firstOrNull { it.id == id }

    suspend fun levelCount(): Int = loadLevels().size
}
