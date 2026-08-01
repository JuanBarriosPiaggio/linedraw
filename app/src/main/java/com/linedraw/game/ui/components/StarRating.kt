package com.linedraw.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linedraw.game.ui.theme.BorderSubtle
import com.linedraw.game.ui.theme.StarGold
import kotlin.math.cos
import kotlin.math.sin

/** 1–3 star rating; filled gold vs. subtle outline. */
@Composable
fun StarRating(
    stars: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 22.dp,
    spacing: Dp = 6.dp,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(3) { index ->
            Star(filled = index < stars, modifier = Modifier.size(starSize))
        }
    }
}

@Composable
private fun Star(filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = starPath(size.minDimension)
        if (filled) {
            drawPath(path, StarGold)
        } else {
            drawPath(path, BorderSubtle, style = Stroke(width = size.minDimension * 0.07f))
        }
    }
}

private fun starPath(side: Float): Path {
    val cx = side / 2f
    val cy = side / 2f
    val outer = side * 0.48f
    val inner = outer * 0.42f
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = Math.PI / 5 * i - Math.PI / 2
        val point = Offset(
            cx + (radius * cos(angle)).toFloat(),
            cy + (radius * sin(angle)).toFloat(),
        )
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}
