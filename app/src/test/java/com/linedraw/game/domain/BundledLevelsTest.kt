package com.linedraw.game.domain

import com.linedraw.game.data.LevelPack
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Validates the shipped levels.json: every level must be solvable under the
 * game's rules and its bundled solution path must be legal and complete.
 */
class BundledLevelsTest {

    private fun loadLevels(): LevelPack {
        val candidates = listOf(
            File("src/main/assets/levels.json"),
            File("app/src/main/assets/levels.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("levels.json not found — run `java tools/LevelGen.java` from the repo root")
        return Json { ignoreUnknownKeys = true }.decodeFromString(file.readText())
    }

    @Test
    fun `ships exactly 60 levels with sequential ids`() {
        val levels = loadLevels().levels
        assertEquals(60, levels.size)
        assertEquals((1..60).toList(), levels.map { it.id })
    }

    @Test
    fun `difficulty tiers ramp from 3x3 to 6x6`() {
        val levels = loadLevels().levels
        assertTrue(levels.filter { it.id <= 15 }.all { it.gridSize == 3 })
        assertTrue(levels.filter { it.id in 16..30 }.all { it.gridSize == 4 })
        assertTrue(levels.filter { it.id in 31..45 }.all { it.gridSize == 5 })
        assertTrue(levels.filter { it.id in 46..60 }.all { it.gridSize == 6 })
    }

    @Test
    fun `every level has a legal, complete bundled solution`() {
        for (level in loadLevels().levels) {
            assertTrue(
                "Level ${level.id} bundled solution is invalid",
                LevelValidator.isSolutionValid(level),
            )
        }
    }

    @Test
    fun `every level is solvable by the independent solver`() {
        for (level in loadLevels().levels) {
            assertTrue("Level ${level.id} is not solvable", LevelValidator.isSolvable(level))
        }
    }

    @Test
    fun `every level fits the solver's bitmask capacity`() {
        for (level in loadLevels().levels) {
            assertTrue("Level ${level.id} has too many dots", level.dots.size <= 36)
            assertTrue("Level ${level.id} has too many edges", level.edges.size <= 63)
        }
    }
}
