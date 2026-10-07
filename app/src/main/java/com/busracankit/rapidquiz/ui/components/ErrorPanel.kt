package com.busracankit.rapidquiz.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.busracankit.rapidquiz.ui.theme.InkMuted
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.RapidShapes
import com.busracankit.rapidquiz.ui.theme.Surface as SurfaceColor

/** Hata kutusu: başlık, açıklama ve en fazla iki buton ("Tekrar dene", "Ana sayfaya dön"). */
@Composable
fun ErrorPanel(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Rounded.CloudOff,
    primaryLabel: String? = null,
    onPrimary: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    Surface(
        shape = RapidShapes.Card,
        color = SurfaceColor,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (!message.isNullOrBlank() && message != title) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
            if (primaryLabel != null) PrimaryButton(primaryLabel, onPrimary)
            if (secondaryLabel != null) SecondaryButton(secondaryLabel, onSecondary)
        }
    }
}
