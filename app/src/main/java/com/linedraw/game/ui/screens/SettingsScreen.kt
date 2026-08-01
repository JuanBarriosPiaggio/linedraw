package com.linedraw.game.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linedraw.game.BuildConfig
import com.linedraw.game.LineDrawApp
import com.linedraw.game.data.Settings
import com.linedraw.game.ui.components.PrimaryButton
import com.linedraw.game.ui.components.SecondaryButton
import com.linedraw.game.ui.theme.AccentCyan
import com.linedraw.game.ui.theme.BorderSubtle
import com.linedraw.game.ui.theme.StarGold
import com.linedraw.game.ui.theme.TextPrimary
import com.linedraw.game.ui.theme.TextSecondary
import com.linedraw.game.ui.theme.VoidBackground
import com.linedraw.game.ui.theme.VoidSurface
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    app: LineDrawApp,
    onBack: () -> Unit,
) {
    val settings by app.progressRepository.settings.collectAsState(initial = Settings())
    val price by app.billingManager.removeAdsPrice.collectAsState()
    val billingAvailable by app.billingManager.billingAvailable.collectAsState()
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBackground)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
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
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(56.dp))
        }

        Spacer(Modifier.height(8.dp))

        SettingToggle(
            label = "Sound",
            checked = settings.soundEnabled,
            onCheckedChange = { scope.launch { app.progressRepository.setSoundEnabled(it) } },
        )
        SettingToggle(
            label = "Haptics",
            checked = settings.hapticsEnabled,
            onCheckedChange = { scope.launch { app.progressRepository.setHapticsEnabled(it) } },
        )

        Spacer(Modifier.height(28.dp))

        // ── Remove Ads (one-time purchase) ───────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VoidSurface, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings.adsRemoved) {
                Text("Ads removed", style = MaterialTheme.typography.titleMedium, color = StarGold)
                Text(
                    "Thanks for supporting Line Draw! Banner and interstitial ads are gone forever. " +
                        "Rewarded hint ads stay available — they're always optional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            } else {
                Text("Remove Ads", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "One-time purchase. Permanently removes banner and interstitial ads. " +
                        "Optional rewarded hints remain available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                PrimaryButton(
                    text = "Remove Ads — ${price ?: "$1.99"}",
                    enabled = billingAvailable || BuildConfig.DEBUG,
                    onClick = { app.billingManager.launchRemoveAdsPurchase(activity) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SecondaryButton(
                text = "Restore purchases",
                onClick = { app.billingManager.restorePurchases() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!billingAvailable) {
                Text(
                    "Google Play Billing unavailable on this device/session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Line Draw v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = BorderSubtle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidBackground,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = VoidSurface,
                uncheckedBorderColor = BorderSubtle,
            ),
        )
    }
}
