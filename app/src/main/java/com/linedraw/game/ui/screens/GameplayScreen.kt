package com.linedraw.game.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linedraw.game.LineDrawApp
import com.linedraw.game.data.Settings
import com.linedraw.game.ui.GameStatus
import com.linedraw.game.ui.GameViewModel
import com.linedraw.game.ui.components.AdBanner
import com.linedraw.game.ui.components.DotGridCanvas
import com.linedraw.game.ui.components.LineLoadingIndicator
import com.linedraw.game.ui.components.PrimaryButton
import com.linedraw.game.ui.components.SecondaryButton
import com.linedraw.game.ui.components.StarRating
import com.linedraw.game.ui.components.SuccessBurst
import com.linedraw.game.ui.theme.StarGold
import com.linedraw.game.ui.theme.TextPrimary
import com.linedraw.game.ui.theme.TextSecondary
import com.linedraw.game.ui.theme.VoidBackground
import com.linedraw.game.ui.theme.VoidSurface

@Composable
fun GameplayScreen(
    app: LineDrawApp,
    initialLevelId: Int,
    onExit: () -> Unit,
) {
    val viewModel: GameViewModel = viewModel(factory = GameViewModel.factory(app))
    LaunchedEffect(initialLevelId) { viewModel.loadLevel(initialLevelId) }

    val state by viewModel.uiState.collectAsState()
    val settings by app.progressRepository.settings.collectAsState(initial = Settings())
    val rewardedReady by app.adManager.rewardedReady.collectAsState()
    val activity = LocalContext.current as Activity

    val level = state.level

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBackground)
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar: back · level number · reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "← Levels",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = onExit)
                        .padding(8.dp),
                )
                Text(
                    if (level != null) "Level ${level.id}" else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                SecondaryButton(text = "Reset", onClick = { viewModel.reset() })
            }

            if (level == null || state.status == GameStatus.Loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LineLoadingIndicator()
                }
            } else {
                DotGridCanvas(
                    level = level,
                    path = state.path,
                    usedEdges = state.usedEdges,
                    hintEdge = state.hintEdge,
                    solved = state.status == GameStatus.Solved,
                    interactive = state.status == GameStatus.Playing || state.status == GameStatus.Stuck,
                    onStart = viewModel::startAt,
                    onMove = viewModel::tryMoveTo,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                )
            }

            // Stuck banner — calm inline nudge, not a modal.
            AnimatedVisibility(
                visible = state.status == GameStatus.Stuck,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(VoidSurface, RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "No moves left from here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(text = "Undo", onClick = viewModel::undo, accent = true)
                }
            }

            if (state.hintUnavailable) {
                Text(
                    "No path to a solve from here — try Undo",
                    style = MaterialTheme.typography.bodySmall,
                    color = StarGold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }

            // Bottom controls: Undo · Hint (rewarded-ad gated)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryButton(
                    text = "Undo",
                    onClick = viewModel::undo,
                    enabled = state.path.isNotEmpty() && state.status != GameStatus.Solved,
                )
                SecondaryButton(
                    text = "Hint",
                    accent = true,
                    enabled = state.status != GameStatus.Solved && rewardedReady,
                    onClick = {
                        app.adManager.showRewarded(
                            activity,
                            onReward = viewModel::grantHint,
                            onUnavailable = {},
                        )
                    },
                )
                if (!rewardedReady) {
                    LineLoadingIndicator(size = 22.dp, strokeWidth = 3.dp)
                }
            }
        }

        // ── Level Complete overlay ───────────────────────────────
        AnimatedVisibility(
            visible = state.status == GameStatus.Solved,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VoidBackground.copy(alpha = 0.94f)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                SuccessBurst()
                Spacer(Modifier.height(12.dp))
                Text("Solved!", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                StarRating(stars = state.starsEarned, starSize = 28.dp)
                Spacer(Modifier.height(28.dp))
                PrimaryButton(
                    text = if (state.isLastLevel) "All levels complete!" else "Next Level",
                    onClick = {
                        val advance: () -> Unit = {
                            if (state.isLastLevel) {
                                onExit()
                            } else {
                                viewModel.loadLevel((level?.id ?: 0) + 1)
                            }
                        }
                        if (state.pendingInterstitial) {
                            app.adManager.showInterstitial(activity) { advance() }
                        } else {
                            advance()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "Replay", onClick = viewModel::replay)
                Spacer(Modifier.weight(1f))
                AdBanner(adsRemoved = settings.adsRemoved)
            }
        }
    }
}
