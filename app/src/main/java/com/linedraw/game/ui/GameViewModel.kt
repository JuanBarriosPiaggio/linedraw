package com.linedraw.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.linedraw.game.LineDrawApp
import com.linedraw.game.data.Level
import com.linedraw.game.domain.PathSolver
import com.linedraw.game.domain.PuzzleGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameStatus { Loading, Playing, Stuck, Solved }

data class GameUiState(
    val level: Level? = null,
    val status: GameStatus = GameStatus.Loading,
    /** Dot ids in draw order. */
    val path: List<Int> = emptyList(),
    /** Indices into the graph's edge list that have been drawn. */
    val usedEdges: Set<Int> = emptySet(),
    val undoCount: Int = 0,
    val usedReset: Boolean = false,
    val usedHint: Boolean = false,
    /** Edge to highlight after a rewarded hint, as a (from, to) dot pair. */
    val hintEdge: Pair<Int, Int>? = null,
    /** Set when the hint search found no way to finish from the current position. */
    val hintUnavailable: Boolean = false,
    val starsEarned: Int = 0,
    /** True when an interstitial should be shown before advancing. */
    val pendingInterstitial: Boolean = false,
    val isLastLevel: Boolean = false,
)

class GameViewModel(private val app: LineDrawApp) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var graph: PuzzleGraph? = null

    fun loadLevel(levelId: Int) {
        _uiState.value = GameUiState(status = GameStatus.Loading)
        viewModelScope.launch {
            val level = app.levelRepository.getLevel(levelId) ?: return@launch
            val total = app.levelRepository.levelCount()
            graph = PuzzleGraph(level)
            _uiState.value = GameUiState(
                level = level,
                status = GameStatus.Playing,
                isLastLevel = levelId >= total,
            )
        }
    }

    /** Starts a new line at [dotId]. Only valid when nothing is drawn yet (free start). */
    fun startAt(dotId: Int) {
        val state = _uiState.value
        if (state.status != GameStatus.Playing || state.path.isNotEmpty()) return
        _uiState.value = state.copy(path = listOf(dotId))
        app.feedback.dotConnected(1)
    }

    /**
     * Attempts to extend the line to [dotId]. Succeeds only when a not-yet-used
     * edge connects it to the current line end.
     */
    fun tryMoveTo(dotId: Int) {
        val state = _uiState.value
        val g = graph ?: return
        if (state.status != GameStatus.Playing && state.status != GameStatus.Stuck) return
        val current = state.path.lastOrNull() ?: return
        if (dotId == current) return

        val edge = g.edgeIndexBetween(current, dotId)
        if (edge < 0 || edge in state.usedEdges) return

        val newPath = state.path + dotId
        val newUsed = state.usedEdges + edge
        val consumedHint =
            state.hintEdge?.let { (a, b) -> (a == current && b == dotId) || (a == dotId && b == current) } == true

        val visited = newPath.toHashSet()
        val solved = visited.size == g.dotCount

        if (solved) {
            onSolved(newPath, newUsed)
            return
        }

        val stuck = g.adjacency[dotId].none { (e, _) -> e !in newUsed }
        _uiState.value = state.copy(
            path = newPath,
            usedEdges = newUsed,
            status = if (stuck) GameStatus.Stuck else GameStatus.Playing,
            hintEdge = if (consumedHint) null else state.hintEdge,
            hintUnavailable = false,
        )
        app.feedback.dotConnected(newPath.size)
        if (stuck) app.feedback.stuck()
    }

    private fun onSolved(path: List<Int>, usedEdges: Set<Int>) {
        val state = _uiState.value
        val level = state.level ?: return
        val stars = when {
            state.undoCount == 0 && !state.usedReset && !state.usedHint -> 3
            state.undoCount <= 3 && !state.usedReset -> 2
            else -> 1
        }
        _uiState.value = state.copy(
            path = path,
            usedEdges = usedEdges,
            status = GameStatus.Solved,
            starsEarned = stars,
            hintEdge = null,
        )
        app.feedback.levelComplete()
        viewModelScope.launch {
            val total = app.levelRepository.levelCount()
            val adsRemoved = app.progressRepository.currentSettings().adsRemoved
            val showAd = app.progressRepository.recordCompletion(level.id, stars, total)
            _uiState.value = _uiState.value.copy(pendingInterstitial = showAd && !adsRemoved)
        }
    }

    fun undo() {
        val state = _uiState.value
        if (state.path.size <= 1) {
            reset(countAsReset = false)
            return
        }
        val g = graph ?: return
        val last = state.path.last()
        val previous = state.path[state.path.size - 2]
        val edge = g.edgeIndexBetween(previous, last)
        _uiState.value = state.copy(
            path = state.path.dropLast(1),
            usedEdges = state.usedEdges - edge,
            status = GameStatus.Playing,
            undoCount = state.undoCount + 1,
            hintEdge = null,
            hintUnavailable = false,
        )
    }

    fun reset(countAsReset: Boolean = true) {
        val state = _uiState.value
        if (state.status == GameStatus.Loading) return
        _uiState.value = state.copy(
            path = emptyList(),
            usedEdges = emptySet(),
            status = GameStatus.Playing,
            usedReset = state.usedReset || (countAsReset && state.path.isNotEmpty()),
            hintEdge = null,
            hintUnavailable = false,
        )
    }

    /** Grants a hint (called after the rewarded ad completes). */
    fun grantHint() {
        val state = _uiState.value
        val level = state.level ?: return
        val g = graph ?: return
        viewModelScope.launch {
            val hint: Pair<Int, Int>? = withContext(Dispatchers.Default) {
                if (state.path.isEmpty()) {
                    val s = level.solution
                    if (s.size >= 2) s[0] to s[1] else null
                } else {
                    PathSolver.nextHint(g, level, state.path, state.usedEdges)
                        ?.let { next -> state.path.last() to next }
                }
            }
            _uiState.value = _uiState.value.copy(
                hintEdge = hint,
                hintUnavailable = hint == null,
                usedHint = _uiState.value.usedHint || hint != null,
            )
        }
    }

    /** Replays the current level from scratch (from the Level Complete overlay). */
    fun replay() {
        val level = _uiState.value.level ?: return
        loadLevel(level.id)
    }

    companion object {
        fun factory(app: LineDrawApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GameViewModel(app) as T
        }
    }
}
