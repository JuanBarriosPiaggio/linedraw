package com.linedraw.game.domain

import com.linedraw.game.data.Dot
import com.linedraw.game.data.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PathSolverTest {

    /** 2x2 square: 0-1 / 2-3 horizontally, 0-2 / 1-3 vertically. */
    private fun squareLevel(edges: List<List<Int>>, solution: List<Int> = emptyList()) = Level(
        id = 1,
        gridSize = 2,
        dots = listOf(Dot(0, 0, 0), Dot(1, 1, 0), Dot(2, 0, 1), Dot(3, 1, 1)),
        edges = edges,
        solution = solution,
    )

    @Test
    fun `edge lookup is order independent`() {
        val graph = PuzzleGraph(squareLevel(listOf(listOf(0, 1), listOf(3, 1))))
        assertEquals(0, graph.edgeIndexBetween(0, 1))
        assertEquals(0, graph.edgeIndexBetween(1, 0))
        assertEquals(1, graph.edgeIndexBetween(1, 3))
        assertEquals(-1, graph.edgeIndexBetween(0, 3))
    }

    @Test
    fun `solver completes a simple open square`() {
        val level = squareLevel(listOf(listOf(0, 1), listOf(1, 3), listOf(3, 2)))
        val graph = PuzzleGraph(level)
        val completion = PathSolver.findCompletion(graph, listOf(0), emptySet())
        assertEquals(listOf(1, 3, 2), completion)
    }

    @Test
    fun `solver respects already used edges`() {
        val level = squareLevel(listOf(listOf(0, 1), listOf(1, 3), listOf(3, 2), listOf(0, 2)))
        val graph = PuzzleGraph(level)
        // Path 0->1 with edge 0 used; must finish 3, 2 without re-using edge 0.
        val completion = PathSolver.findCompletion(graph, listOf(0, 1), setOf(0))
        assertNotNull(completion)
        assertEquals(listOf(3, 2), completion)
    }

    @Test
    fun `solver returns null when no completion exists`() {
        // Only one edge: 0-1. Dots 2 and 3 are unreachable.
        val level = squareLevel(listOf(listOf(0, 1)))
        val graph = PuzzleGraph(level)
        assertNull(PathSolver.findCompletion(graph, listOf(0, 1), setOf(0)))
    }

    @Test
    fun `solver allows revisiting dots but not edges`() {
        // Lollipop: leaf 1 attached to a triangle 0-2-3. Starting at 1 the only
        // way to visit everything is 1->0->2->3->0, passing through dot 0 twice
        // over two different edges.
        val level = Level(
            id = 1,
            gridSize = 3,
            dots = listOf(Dot(0, 1, 1), Dot(1, 0, 1), Dot(2, 2, 1), Dot(3, 1, 0)),
            edges = listOf(listOf(0, 1), listOf(0, 2), listOf(2, 3), listOf(3, 0)),
        )
        val graph = PuzzleGraph(level)
        val completion = PathSolver.findCompletion(graph, listOf(1), emptySet())
        assertNotNull(completion)
        // Verify legality of the returned continuation.
        val path = listOf(1) + completion!!
        val used = mutableSetOf<Int>()
        for (i in 1 until path.size) {
            val edge = graph.edgeIndexBetween(path[i - 1], path[i])
            assertTrue("edge exists", edge >= 0)
            assertTrue("edge not reused", used.add(edge))
        }
        assertEquals(setOf(0, 1, 2, 3), path.toSet())
    }

    @Test
    fun `empty completion returned when everything already visited`() {
        val level = squareLevel(listOf(listOf(0, 1), listOf(1, 3), listOf(3, 2)))
        val graph = PuzzleGraph(level)
        val completion = PathSolver.findCompletion(graph, listOf(0, 1, 3, 2), setOf(0, 1, 2))
        assertEquals(emptyList<Int>(), completion)
    }

    @Test
    fun `validator rejects disconnected level`() {
        val level = squareLevel(listOf(listOf(0, 1)))
        assertFalse(LevelValidator.isSolvable(level))
    }

    @Test
    fun `validator accepts solvable level`() {
        val level = squareLevel(listOf(listOf(0, 1), listOf(1, 3), listOf(3, 2)))
        assertTrue(LevelValidator.isSolvable(level))
    }

    @Test
    fun `hint suggests a dot that leads to a solve`() {
        val level = squareLevel(
            edges = listOf(listOf(0, 1), listOf(1, 3), listOf(3, 2)),
            solution = listOf(0, 1, 3, 2),
        )
        val graph = PuzzleGraph(level)
        val hint = PathSolver.nextHint(graph, level, listOf(0), emptySet())
        assertEquals(1, hint)
    }
}
