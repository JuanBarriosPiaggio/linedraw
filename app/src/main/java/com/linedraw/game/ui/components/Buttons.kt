package com.linedraw.game.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linedraw.game.ui.theme.AccentCyan
import com.linedraw.game.ui.theme.BorderSubtle
import com.linedraw.game.ui.theme.TextPrimary
import com.linedraw.game.ui.theme.VoidBackground

/** Filled cyan pill — primary CTA ("Play", "Next Level"). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentCyan,
            contentColor = VoidBackground,
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 13.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Outlined pill — secondary actions ("Undo", "Reset", "Replay"). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, if (accent) AccentCyan else BorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (accent) AccentCyan else TextPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
