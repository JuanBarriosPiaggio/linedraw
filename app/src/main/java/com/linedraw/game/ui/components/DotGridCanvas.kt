package com.linedraw.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.linedraw.game.data.Level
import com.linedraw.game.ui.theme.AccentCyan
import com.linedraw.game.ui.theme.DotIdle
import com.linedraw.game.ui.theme.StarGold

private fun dotPositions(level: Level, size: IntSize): List<Offset> {
    val side = minOf(size.width, size.height).toFloat()
    val pad = side * 0.11f
    val cells = (level.gridSize - 1).coerceAtLeast(1)
    val step = (side - 2 * pad) / cells
    val offsetX = (size.width - side) / 2f
    val offsetY = (size.height - side) / 2f
    return level.dots.map { dot ->
        Offset(offsetX + pad + dot.x * step, offsetY + pad + dot.y * step)
    }
}

private fun nearestDot(positions: List<Offset>, point: Offset, radius: Float): Int? {
    var best = -1
    var bestDistance = Float.MAX_VALUE
    positions.forEachIndexed { index, position ->
        val d = (position - point).getDistance()
        if (d < bestDistance) {
            bestDistance = d
            best = index
        }
    }
    return if (best >= 0 && bestDistance <= radius) best else null
}

/**
 * The puzzle surface: renders dots + valid edges and lets the player drag one
 * continuous line through the grid. Segment endpoints snap to dots; validation
 * (edge exists / not reused) is delegated to the callbacks' owner.
 */
@Composable
fun DotGridCanvas(
    level: Level,
    path: List<Int>,
    usedEdges: Set<Int>,
    hintEdge: Pair<Int, Int>?,
    solved: Boolean,
    interactive: Boolean,
    onStart: (Int) -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fingerPosition by remember(level.id) { mutableStateOf<Offset?>(null) }

    val pulse by rememberInfiniteTransition(label = "dotPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotPulse",
    )

    val normalizedEdges = remember(level.id) {
        level.edges.map { (a, b) -> if (a < b) a to b else b to a }
    }

    Canvas(
        modifier = modifier
            .pointerInput(level.id, interactive) {
                if (!interactive) return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        val positions = dotPositions(level, size)
                        val radius = snapRadius(level, size)
                        nearestDot(positions, start, radius)?.let { dot ->
                            if (path.isEmpty()) onStart(dot)
                        }
                        fingerPosition = start
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        fingerPosition = change.position
                        val positions = dotPositions(level, size)
                        val radius = snapRadius(level, size)
                        nearestDot(positions, change.position, radius)?.let { dot ->
                            onMove(dot)
                        }
                    },
                    onDragEnd = { fingerPosition = null },
                    onDragCancel = { fingerPosition = null },
                )
            }
            .pointerInput(level.id, interactive) {
                if (!interactive) return@pointerInput
                detectTapGestures { tap ->
                    val positions = dotPositions(level, size)
                    val radius = snapRadius(level, size)
                    nearestDot(positions, tap, radius)?.let { dot ->
                        if (path.isEmpty()) onStart(dot) else onMove(dot)
                    }
                }
            },
    ) {
        val positions = dotPositions(level, IntSize(size.width.toInt(), size.height.toInt()))
        val edgeStroke = 3.dp.toPx()
        val lineStroke = 8.dp.toPx()
        val glowStroke = 18.dp.toPx()

        // Valid but not-yet-used edges — faint scaffolding.
        normalizedEdges.forEachIndexed { index, (a, b) ->
            if (index !in usedEdges) {
                drawLine(
                    color = Color(0xFF262B33),
                    start = positions[a],
                    end = positions[b],
                    strokeWidth = edgeStroke,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Rewarded hint: dashed gold edge.
        hintEdge?.let { (a, b) ->
            if (a in positions.indices && b in positions.indices) {
                drawLine(
                    color = StarGold,
                    start = positions[a],
                    end = positions[b],
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 8.dp.toPx()),
                        pulse * 14.dp.toPx(),
                    ),
                )
            }
        }

        // The drawn line: soft glow underlay + solid stroke on top.
        if (path.size >= 2) {
            val linePath = Path().apply {
                moveTo(positions[path[0]].x, positions[path[0]].y)
                for (i in 1 until path.size) {
                    lineTo(positions[path[i]].x, positions[path[i]].y)
                }
            }
            val glowAlpha = if (solved) 0.30f + pulse * 0.35f else 0.30f
            drawPath(
                path = linePath,
                color = AccentCyan.copy(alpha = glowAlpha),
                style = Stroke(width = glowStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = linePath,
                color = AccentCyan,
                style = Stroke(width = lineStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        // Rubber band from line end to the finger while dragging.
        val finger = fingerPosition
        if (!solved && finger != null && path.isNotEmpty()) {
            drawLine(
                color = AccentCyan.copy(alpha = 0.45f),
                start = positions[path.last()],
                end = finger,
                strokeWidth = lineStroke,
                cap = StrokeCap.Round,
            )
        }

        // Dots.
        val visited = path.toHashSet()
        val currentDot = path.lastOrNull()
        positions.forEachIndexed { index, position ->
            when {
                index == currentDot && !solved -> {
                    drawCircle(
                        color = AccentCyan.copy(alpha = 0.25f + pulse * 0.25f),
                        radius = 14.dp.toPx() + pulse * 3.dp.toPx(),
                        center = position,
                    )
                    drawCircle(color = AccentCyan, radius = 9.dp.toPx(), center = position)
                }
                index in visited -> {
                    drawCircle(color = AccentCyan, radius = 7.dp.toPx(), center = position)
                }
                else -> {
                    drawCircle(
                        color = DotIdle,
                        radius = 6.dp.toPx(),
                        center = position,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
        }
    }
}

private fun snapRadius(level: Level, size: IntSize): Float {
    val side = minOf(size.width, size.height).toFloat()
    val cells = (level.gridSize - 1).coerceAtLeast(1)
    val step = (side - 2 * side * 0.11f) / cells
    return step * 0.38f
}
