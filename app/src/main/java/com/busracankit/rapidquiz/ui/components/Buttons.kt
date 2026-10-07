package com.busracankit.rapidquiz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.RapidShapes

/** Ana buton: mor dolgu, tam genişlik, 16 köşe. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RapidShapes.Button,
        colors = ButtonDefaults.buttonColors(containerColor = Primary),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/** İkincil buton: mor çerçeve. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RapidShapes.Button,
        border = BorderStroke(2.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

/** Yazı buton (ör. "Skor tablosunu gör"). */
@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        shape = RapidShapes.Button,
        colors = ButtonDefaults.textButtonColors(contentColor = Primary),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
