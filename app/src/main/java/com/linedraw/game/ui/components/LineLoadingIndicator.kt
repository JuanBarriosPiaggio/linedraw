package com.linedraw.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linedraw.game.ui.theme.AccentCyan

/**
 * Brand loading indicator: a line segment chasing around a rounded square —
 * the "line" motif instead of a generic circular spinner.
 * Use size 72.dp for full-screen states, 24.dp inline (e.g. waiting for an ad).
 */
@Composable
fun LineLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 5.dp,
) {
    val phase by rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "loading",
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val inset = stroke
        val rect = RoundRect(
            left = inset,
            top = inset,
            right = this.size.width - inset,
            bottom = this.size.height - inset,
            cornerRadius = CornerRadius(this.size.minDimension * 0.22f),
        )
        val path = Path().apply { addRoundRect(rect) }
        val measure = android.graphics.PathMeasure(path.asAndroidPath(), false)
        val length = measure.length
        val segment = length * 0.26f
        drawPath(
            path = path,
            color = AccentCyan,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(segment, length - segment),
                    -phase * length,
                ),
            ),
        )
    }
}
