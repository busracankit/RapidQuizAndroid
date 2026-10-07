package com.busracankit.rapidquiz.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.ui.theme.Danger
import com.busracankit.rapidquiz.ui.theme.Ink
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.RapidShapes
import com.busracankit.rapidquiz.ui.theme.Success
import com.busracankit.rapidquiz.ui.theme.SuccessText
import com.busracankit.rapidquiz.util.LocalReduceMotion
import com.busracankit.rapidquiz.ui.theme.Surface as SurfaceColor

/** Şık durumları (docs/PROJE.md › 5.3). */
enum class ChoiceState { Idle, Pending, Correct, Wrong, Reveal, Dimmed }

private data class ChoiceColors(
    val container: Color,
    val border: Color,
    val badge: Color,
    val badgeText: Color,
)

private fun colorsFor(state: ChoiceState): ChoiceColors = when (state) {
    ChoiceState.Idle -> ChoiceColors(SurfaceColor, Ink.copy(alpha = 0.12f), Primary.copy(alpha = 0.12f), Primary)
    ChoiceState.Pending -> ChoiceColors(Primary.copy(alpha = 0.10f), Primary, Primary, Color.White)
    ChoiceState.Correct -> ChoiceColors(Success.copy(alpha = 0.18f), Success, SuccessText, Color.White)
    ChoiceState.Wrong -> ChoiceColors(Danger.copy(alpha = 0.14f), Danger, Danger, Color.White)
    ChoiceState.Reveal -> ChoiceColors(SurfaceColor, Success, Success.copy(alpha = 0.18f), SuccessText)
    ChoiceState.Dimmed -> ChoiceColors(SurfaceColor, Ink.copy(alpha = 0.08f), Ink.copy(alpha = 0.06f), Ink)
}

/**
 * Şık butonu: tam genişlik, en az 56 yükseklik, solda A/B/C/D rozeti, kalın metin.
 * Doğru/yanlış yalnızca renkle değil ✓/✕ ikonuyla da belirtilir.
 */
@Composable
fun ChoiceButton(
    letter: String,
    text: String,
    state: ChoiceState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val colors = colorsFor(state)
    val container by animateColorAsState(colors.container, tween(if (reduceMotion) 0 else 150), label = "choiceBg")
    val border by animateColorAsState(colors.border, tween(if (reduceMotion) 0 else 150), label = "choiceBorder")

    // Yanlışta hafif sallanma, doğruda kısa pulse
    val shake = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (reduceMotion) return@LaunchedEffect
        when (state) {
            ChoiceState.Wrong -> for (x in listOf(-14f, 12f, -8f, 5f, 0f)) shake.animateTo(x, tween(55))
            ChoiceState.Correct -> {
                scale.animateTo(1.04f, tween(110))
                scale.animateTo(1f, tween(160))
            }
            else -> Unit
        }
    }

    val a11yLabel = stringResource(R.string.choice_a11y, letter, text)
    val a11yState = when (state) {
        ChoiceState.Correct -> stringResource(R.string.correct)
        ChoiceState.Wrong -> stringResource(R.string.wrong)
        ChoiceState.Reveal -> stringResource(R.string.correct_answer_was, text)
        else -> null
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RapidShapes.Button,
        color = container,
        contentColor = Ink,
        border = BorderStroke(2.dp, border),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer {
                translationX = shake.value * density
                scaleX = scale.value
                scaleY = scale.value
                alpha = if (state == ChoiceState.Dimmed) 0.5f else 1f
            }
            .semantics {
                contentDescription = a11yLabel
                if (a11yState != null) stateDescription = a11yState
            },
    ) {
        Row(
            modifier = Modifier
                .clearAndSetSemantics { }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.badge),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, color = colors.badgeText, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            when (state) {
                ChoiceState.Correct, ChoiceState.Reveal ->
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = SuccessText)
                ChoiceState.Wrong ->
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = Danger)
                ChoiceState.Pending ->
                    CircularProgressIndicator(Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                else -> Unit
            }
        }
    }
}
