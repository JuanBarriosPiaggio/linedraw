package com.linedraw.game.domain

import com.linedraw.game.data.Level

/**
 * Graph view of a [Level]. Dots are nodes, edges are undirected connections.
 * Rules: an edge may be used at most once; dots may be re-visited; the puzzle
 * is solved when every dot has been visited at least once.
 *
 * Edge and dot sets are small (max 6x6 grid = 36 dots, <=64 edges) so both are
 * tracked as Long bitmasks.
 */
class PuzzleGraph(level: Level) {

    val dotCount: Int = level.dots.size

    /** Normalized (small id, big id) pairs, index = edge id. */
    val edges: List<Pair<Int, Int>> = level.edges.map { (a, b) ->
        if (a < b) a to b else b to a
    }

    /** dot id -> list of (edgeIndex, neighbour dot id) */
    val adjacency: Array<List<Pair<Int, Int>>> = run {
        val lists = Array(dotCount) { mutableListOf<Pair<Int, Int>>() }
        edges.forEachIndexed { index, (a, b) ->
            lists[a].add(index to b)
            lists[b].add(index to a)
        }
        lists.map { it.toList() }.toTypedArray()
    }

    val allDotsMask: Long = if (dotCount >= 64) -1L else (1L shl dotCount) - 1

    fun edgeIndexBetween(a: Int, b: Int): Int {
        val key = if (a < b) a to b else b to a
        return edges.indexOf(key)
    }
}

object PathSolver {

    const val DEFAULT_BUDGET = 400_000

    /**
     * Searches for a continuation that visits all remaining dots, starting from the
     * end of [path] with [usedEdges] already consumed.
     *
     * @return the list of dot ids to visit next (not including the current dot),
     *         or null if none was found within the iteration budget.
     */
    fun findCompletion(
        graph: PuzzleGraph,
        path: List<Int>,
        usedEdges: Set<Int>,
        budget: Int = DEFAULT_BUDGET,
    ): List<Int>? {
        if (path.isEmpty()) return null
        var visitedMask = 0L
        for (dot in path) visitedMask = visitedMask or (1L shl dot)
        var usedMask = 0L
        for (e in usedEdges) usedMask = usedMask or (1L shl e)

        if (visitedMask == graph.allDotsMask) return emptyList()

        val result = mutableListOf<Int>()
        val iterations = intArrayOf(0)
        val found = dfs(graph, path.last(), usedMask, visitedMask, result, iterations, budget)
        return if (found) result else null
    }

    private fun dfs(
        graph: PuzzleGraph,
        current: Int,
        usedMask: Long,
        visitedMask: Long,
        out: MutableList<Int>,
        iterations: IntArray,
        budget: Int,
    ): Boolean {
        if (visitedMask == graph.allDotsMask) return true
        if (iterations[0]++ > budget) return false

        // Prefer moves that reach unvisited dots first — dramatically prunes the search.
        val neighbours = graph.adjacency[current]
            .filter { (edge, _) -> usedMask and (1L shl edge) == 0L }
            .sortedBy { (_, next) -> if (visitedMask and (1L shl next) == 0L) 0 else 1 }

        for ((edge, next) in neighbours) {
            out.add(next)
            val solved = dfs(
                graph,
                next,
                usedMask or (1L shl edge),
                visitedMask or (1L shl next),
                out,
                iterations,
                budget,
            )
            if (solved) return true
            out.removeAt(out.size - 1)
        }
        return false
    }

    /**
     * Suggests the next dot to move to from the current game state, for the hint system.
     * Falls back to the level's stored solution when the search budget is exhausted.
     */
    fun nextHint(
        graph: PuzzleGraph,
        level: Level,
        path: List<Int>,
        usedEdges: Set<Int>,
    ): Int? {
        if (path.isEmpty()) {
            return level.solution.firstOrNull() ?: 0
        }
        val completion = findCompletion(graph, path, usedEdges)
        if (completion != null) return completion.firstOrNull()

        // Search failed (deviated into an unsolvable branch) — no valid hint from here.
        return null
    }
}

object LevelValidator {

    /**
     * A level is valid when at least one starting dot allows a path that visits
     * every dot without reusing an edge.
     */
    fun isSolvable(level: Level, budget: Int = PathSolver.DEFAULT_BUDGET): Boolean {
        val graph = PuzzleGraph(level)
        if (level.dots.isEmpty() || graph.edges.isEmpty()) return false
        for (start in level.dots.indices) {
            val completion = PathSolver.findCompletion(graph, listOf(start), emptySet(), budget)
            if (completion != null) return true
        }
        return false
    }

    /** Verifies that the level's bundled solution path is actually legal and complete. */
    fun isSolutionValid(level: Level): Boolean {
        val solution = level.solution
        if (solution.isEmpty()) return false
        val graph = PuzzleGraph(level)
        val used = mutableSetOf<Int>()
        for (i in 1 until solution.size) {
            val edge = graph.edgeIndexBetween(solution[i - 1], solution[i])
            if (edge < 0 || !used.add(edge)) return false
        }
        return solution.toSet().size == level.dots.size &&
            solution.all { it in level.dots.indices }
    }
}
