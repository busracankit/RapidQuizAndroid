package com.busracankit.rapidquiz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Yalnızca açık tema; koyu tema ve dinamik renk (Material You) bilerek yok.
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    tertiary = Success,
    background = Bg,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceContainer = Surface,
    surfaceContainerLow = Surface,
    surfaceContainerHigh = Surface,
    error = Danger,
    onError = Color.White,
)

/** Köşeler: kartlar 24, butonlar 16. */
object RapidShapes {
    val Card = RoundedCornerShape(24.dp)
    val Button = RoundedCornerShape(16.dp)
}

private val AppShapes = Shapes(
    medium = RapidShapes.Button,
    large = RapidShapes.Card,
)

@Composable
fun RapidQuizTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
