package com.busracankit.rapidquiz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.ui.theme.Danger
import com.busracankit.rapidquiz.ui.theme.Ink
import com.busracankit.rapidquiz.ui.theme.SpaceGrotesk
import com.busracankit.rapidquiz.ui.theme.Success
import com.busracankit.rapidquiz.ui.theme.Warning
import kotlin.math.ceil

/** Saniye gösterimi: 4001 ms → 5, 1 ms → 1, 0 → 0. */
fun secondsLeft(remainingMs: Long): Int = ceil(remainingMs.coerceAtLeast(0) / 1000.0).toInt()

/** Halka rengi: > 2 sn yeşil, 2–1 sn sarı, son 1 sn kırmızı. */
fun ringColor(remainingMs: Long) = when {
    remainingMs > 2000 -> Success
    remainingMs > 1000 -> Warning
    else -> Danger
}

/**
 * Dairesel geri sayım. Değeri dışarıdan (her karede monoton saatten) alır; kendisi zaman tutmaz.
 * Ekran okuyucu "3 saniye kaldı" diye okur.
 */
@Composable
fun CountdownRing(
    remainingMs: Long,
    totalMs: Long,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val fraction = if (totalMs <= 0) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
    val color = ringColor(remainingMs)
    val seconds = secondsLeft(remainingMs)
    val description = stringResource(R.string.seconds_left, seconds)

    Box(
        modifier = modifier.size(size).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = Ink.copy(alpha = 0.08f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = seconds.toString(),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = Ink,
        )
    }
}
