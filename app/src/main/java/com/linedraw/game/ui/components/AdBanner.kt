package com.linedraw.game.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.linedraw.game.BuildConfig
import com.linedraw.game.ads.AdConfig
import com.linedraw.game.ui.theme.TextSecondary
import com.linedraw.game.ui.theme.VoidSurface

/**
 * Adaptive banner slot (Level Select + Level Complete only — never during
 * active gameplay). Renders nothing when ads were removed via IAP; renders a
 * placeholder in mock/debug mode.
 */
@SuppressLint("MissingPermission")
@Composable
fun AdBanner(adsRemoved: Boolean, modifier: Modifier = Modifier) {
    if (adsRemoved) return

    if (BuildConfig.USE_MOCK_ADS) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(VoidSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text("Ad banner (mock)", color = TextSecondary)
        }
        return
    }

    val adWidthDp = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp),
                )
                adUnitId = AdConfig.BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
