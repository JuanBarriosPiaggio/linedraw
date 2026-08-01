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
    fun `difficulty ramps quickly after the 5-level tutorial`() {
        // Named shape levels (Star, Fish, ...) sit outside the square-grid curve.
        val levels = loadLevels().levels.filter { it.name == null }
        assertTrue("Levels 1-5 are the 3x3 tutorial", levels.filter { it.id <= 5 }.all { it.gridSize == 3 })
        assertTrue(levels.filter { it.id in 6..14 }.all { it.gridSize == 4 })
        assertTrue(levels.filter { it.id in 15..26 }.all { it.gridSize == 5 })
        assertTrue(levels.filter { it.id >= 27 }.all { it.gridSize == 6 })
        // Grid size never shrinks as levels progress.
        assertEquals(levels.map { it.gridSize }, levels.map { it.gridSize }.sorted())
    }

    @Test
    fun `shape levels are present with geometric shapes before animals`() {
        val shapes = loadLevels().levels.filter { it.name != null }.associate { it.id to it.name }
        assertEquals(
            mapOf(
                8 to "Star", 14 to "Octagon", 20 to "Diamond", 26 to "Rings",
                34 to "Butterfly", 40 to "Fish", 48 to "Bird", 56 to "Cat",
            ),
            shapes,
        )
    }

    @Test
    fun `no level is a rotation or reflection of another`() {
        val seen = mutableMapOf<String, Int>()
        for (level in loadLevels().levels) {
            val canonical = canonicalForm(level.gridSize, level.edges)
            val clash = seen.put(canonical, level.id)
            assertTrue("Level ${level.id} duplicates level $clash up to symmetry", clash == null)
        }
    }

    /** Mirrors tools/LevelGen.java: minimal edge-set fingerprint over the 8 square symmetries. */
    private fun canonicalForm(gridSize: Int, edges: List<List<Int>>): String {
        fun transformDot(dot: Int, t: Int): Int {
            val m = gridSize - 1
            val x = dot % gridSize
            val y = dot / gridSize
            var (nx, ny) = when (t % 4) {
                1 -> (m - y) to x
                2 -> (m - x) to (m - y)
                3 -> y to (m - x)
                else -> x to y
            }
            if (t >= 4) nx = m - nx
            return ny * gridSize + nx
        }
        return (0 until 8).minOf { t ->
            edges
                .map { (a, b) ->
                    val ta = transformDot(a, t)
                    val tb = transformDot(b, t)
                    "${minOf(ta, tb)}-${maxOf(ta, tb)}"
                }
                .sorted()
                .joinToString(",", prefix = "$gridSize|")
        }
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
