package com.linedraw.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.linedraw.game.LineDrawApp
import com.linedraw.game.data.Level
import com.linedraw.game.ui.components.AdBanner
import com.linedraw.game.ui.components.StarRating
import com.linedraw.game.ui.theme.AccentCyan
import com.linedraw.game.ui.theme.DotIdle
import com.linedraw.game.ui.theme.TextPrimary
import com.linedraw.game.ui.theme.TextSecondary
import com.linedraw.game.ui.theme.VoidBackground
import com.linedraw.game.ui.theme.VoidSurface

@Composable
fun LevelSelectScreen(
    app: LineDrawApp,
    onLevelClick: (Int) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val levels by produceState<List<Level>>(initialValue = emptyList(), app) {
        value = app.levelRepository.loadLevels()
    }
    val unlockedLevel by app.progressRepository.unlockedLevel.collectAsState(initial = 1)
    val settings by app.progressRepository.settings.collectAsState(
        initial = com.linedraw.game.data.Settings(),
    )
    val starsMap by produceState<Map<Int, Int>>(initialValue = emptyMap(), levels) {
        if (levels.isNotEmpty()) {
            app.progressRepository.starsMap(levels.map { it.id }).collect { value = it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBackground)
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "← Back",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(8.dp),
            )
            Text(
                "Levels",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onSettings) {
                Icon(SettingsGearIcon, contentDescription = "Settings", tint = TextSecondary)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(levels, key = { it.id }) { level ->
                val locked = level.id > unlockedLevel
                val stars = starsMap[level.id] ?: 0
                LevelTile(
                    levelId = level.id,
                    locked = locked,
                    stars = stars,
                    isCurrent = level.id == unlockedLevel,
                    onClick = { if (!locked) onLevelClick(level.id) },
                )
            }
        }

        AdBanner(adsRemoved = settings.adsRemoved)
    }
}

@Composable
private fun LevelTile(
    levelId: Int,
    locked: Boolean,
    stars: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(VoidSurface, shape)
            .border(
                width = 1.5.dp,
                color = if (isCurrent && !locked) AccentCyan else Color.Transparent,
                shape = shape,
            )
            .clickable(enabled = !locked, onClick = onClick)
            .alpha(if (locked) 0.4f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (locked) {
                // Simple lock glyph: a square outline.
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(2.dp, DotIdle, RoundedCornerShape(4.dp)),
                )
            } else {
                Text(
                    "$levelId",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (stars > 0) AccentCyan else TextPrimary,
                )
                if (stars > 0) {
                    StarRating(stars = stars, starSize = 9.dp, spacing = 2.dp)
                }
            }
        }
    }
}
