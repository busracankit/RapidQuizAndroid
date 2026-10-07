package com.busracankit.rapidquiz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.busracankit.rapidquiz.util.LocalReduceMotion
import com.busracankit.rapidquiz.util.rememberSystemReduceMotion

// Yalnızca açık tema; koyu tema ve dinamik renk (Material You) bilerek yok.
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary.copy(alpha = 0.12f),
    onPrimaryContainer = Ink,
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = Primary.copy(alpha = 0.12f),
    onSecondaryContainer = Ink,
    tertiary = Success,
    background = Bg,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
    surfaceVariant = Bg,
    surfaceContainer = Surface,
    surfaceContainerLow = Surface,
    surfaceContainerHigh = Surface,
    surfaceContainerHighest = Surface,
    outline = Ink.copy(alpha = 0.25f),
    outlineVariant = Ink.copy(alpha = 0.12f),
    error = DangerText,
    onError = Color.White,
)

/** Köşeler: kartlar 24, butonlar 16. */
object RapidShapes {
    val Card = RoundedCornerShape(24.dp)
    val Button = RoundedCornerShape(16.dp)
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RapidShapes.Button,
    large = RapidShapes.Card,
)

@Composable
fun RapidQuizTheme(
    reduceMotion: Boolean = rememberSystemReduceMotion(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
