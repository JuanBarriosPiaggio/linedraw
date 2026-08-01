package com.linedraw.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linedraw.game.ui.theme.AccentCyan
import com.linedraw.game.ui.theme.StarGold
import kotlin.math.cos
import kotlin.math.sin

/**
 * Solve celebration: the cyan dot pops in, then six thin gold rays fire outward
 * and fade — a satisfying "click", not fireworks. ~600ms one-shot.
 */
@Composable
fun SuccessBurst(modifier: Modifier = Modifier, size: Dp = 100.dp) {
    val pop = remember { Animatable(0f) }
    val rays = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        pop.animateTo(1f, tween(280))
        rays.animateTo(1f, tween(420))
    }

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            color = AccentCyan,
            radius = this.size.minDimension * 0.17f * pop.value,
            center = center,
        )
        if (rays.value > 0f) {
            val progress = rays.value
            val alpha = 1f - progress
            val innerRadius = this.size.minDimension * (0.26f + 0.16f * progress)
            val outerRadius = innerRadius + this.size.minDimension * 0.13f
            for (i in 0 until 6) {
                val angle = Math.PI / 3 * i - Math.PI / 2
                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                drawLine(
                    color = StarGold.copy(alpha = alpha),
                    start = center + direction * innerRadius,
                    end = center + direction * outerRadius,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
