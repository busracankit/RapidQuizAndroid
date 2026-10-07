package com.busracankit.rapidquiz.ui.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.ui.components.ErrorPanel
import com.busracankit.rapidquiz.ui.components.PrimaryButton
import com.busracankit.rapidquiz.ui.components.SecondaryButton
import com.busracankit.rapidquiz.ui.components.TertiaryButton
import com.busracankit.rapidquiz.ui.theme.AccentText
import com.busracankit.rapidquiz.ui.theme.Bg
import com.busracankit.rapidquiz.ui.theme.InkMuted
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.RapidQuizTheme
import com.busracankit.rapidquiz.ui.theme.RapidShapes
import com.busracankit.rapidquiz.ui.theme.SpaceGrotesk
import com.busracankit.rapidquiz.ui.theme.SuccessText
import com.busracankit.rapidquiz.ui.theme.parseCategoryColor
import com.busracankit.rapidquiz.util.LocalReduceMotion
import com.busracankit.rapidquiz.util.asString
import com.busracankit.rapidquiz.util.formatSeconds
import com.busracankit.rapidquiz.ui.theme.Surface as SurfaceColor

@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onPlayAgain: (slug: String) -> Unit,
    onOtherCategory: () -> Unit,
    onLeaderboard: (LeaderboardTarget) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            onLeaderboard(it)
            viewModel.onNavigated()
        }
    }

    ResultContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onSave = viewModel::save,
        onRetry = viewModel::load,
        onPlayAgain = { viewModel.categorySlug?.let(onPlayAgain) ?: onOtherCategory() },
        onOtherCategory = onOtherCategory,
        onSeeLeaderboard = { viewModel.leaderboardTarget()?.let(onLeaderboard) },
    )
}

@Composable
fun ResultContent(
    state: ResultUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onPlayAgain: () -> Unit,
    onOtherCategory: () -> Unit,
    onSeeLeaderboard: () -> Unit,
) {
    Scaffold(containerColor = Bg) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            val result = state.result
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }

                result != null && state.error == null -> ResultBody(
                    result = result,
                    state = state,
                    onNameChange = onNameChange,
                    onSave = onSave,
                    onPlayAgain = onPlayAgain,
                    onOtherCategory = onOtherCategory,
                    onSeeLeaderboard = onSeeLeaderboard,
                )

                else -> Box(
                    Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorPanel(
                        title = (state.error?.asString() ?: stringResource(R.string.error_generic)),
                        primaryLabel = if (state.sessionLost) null else stringResource(R.string.retry),
                        onPrimary = onRetry,
                        secondaryLabel = stringResource(R.string.back_home),
                        onSecondary = onOtherCategory,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultBody(
    result: GameResult,
    state: ResultUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onPlayAgain: () -> Unit,
    onOtherCategory: () -> Unit,
    onSeeLeaderboard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryChip(result.category)
        Text(
            stringResource(R.string.result_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
        AnimatedScore(score = result.score)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = stringResource(R.string.result_correct),
                value = stringResource(R.string.progress, result.correctCount, result.totalQuestions),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.result_time),
                value = stringResource(R.string.seconds_fmt, formatSeconds(result.totalTimeMs)),
                modifier = Modifier.weight(1f),
            )
        }

        Surface(shape = RapidShapes.Card, color = SurfaceColor, shadowElevation = 2.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (result.scoreSaved) {
                    Text(
                        stringResource(R.string.already_saved, result.playerName.orEmpty()),
                        style = MaterialTheme.typography.titleMedium,
                        color = SuccessText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    NameForm(state = state, onNameChange = onNameChange, onSave = onSave)
                }
            }
        }

        SecondaryButton(stringResource(R.string.play_again), onClick = onPlayAgain)
        SecondaryButton(stringResource(R.string.other_category), onClick = onOtherCategory)
        TertiaryButton(stringResource(R.string.see_leaderboard), onClick = onSeeLeaderboard)
    }
}

@Composable
private fun NameForm(state: ResultUiState, onNameChange: (String) -> Unit, onSave: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val nameError = state.nameError
    val submit = {
        focusManager.clearFocus()
        onSave()
    }
    Text(stringResource(R.string.name_prompt), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChange,
        placeholder = { Text(stringResource(R.string.name_placeholder)) },
        singleLine = true,
        enabled = !state.saving,
        isError = nameError != null,
        supportingText = if (nameError != null) {
            { Text(nameError.asString()) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        shape = RapidShapes.Button,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            cursorColor = Primary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton(
        text = stringResource(if (state.saving) R.string.saving else R.string.save),
        onClick = submit,
        enabled = !state.saving,
    )
}

/** Sayarak artan büyük puan. Hareketi azalt açıksa doğrudan son değer. */
@Composable
private fun AnimatedScore(score: Int) {
    val reduceMotion = LocalReduceMotion.current
    val animated = remember { Animatable(if (reduceMotion) score.toFloat() else 0f) }
    LaunchedEffect(score, reduceMotion) {
        if (reduceMotion) {
            animated.snapTo(score.toFloat())
        } else {
            animated.animateTo(score.toFloat(), tween(durationMillis = 1200, easing = FastOutSlowInEasing))
        }
    }
    val description = "$score ${stringResource(R.string.points)}"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Text(
            animated.value.toInt().toString(),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            color = AccentText,
        )
        Text(stringResource(R.string.points), style = MaterialTheme.typography.titleMedium, color = InkMuted)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RapidShapes.Card,
        color = SurfaceColor,
        shadowElevation = 2.dp,
        modifier = modifier.semantics(mergeDescendants = true) { },
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun CategoryChip(category: CategoryBrief) {
    val color = parseCategoryColor(category.color)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(category.name, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ResultPreview() {
    RapidQuizTheme(reduceMotion = true) {
        ResultContent(
            state = ResultUiState(
                loading = false,
                result = GameResult(
                    sessionId = "x",
                    category = CategoryBrief("yazilim", "Yazılım", "#3B82F6"),
                    score = 2140,
                    maxScore = 3000,
                    correctCount = 16,
                    totalQuestions = 20,
                    totalTimeMs = 41230,
                    scoreSaved = false,
                ),
            ),
            onNameChange = {},
            onSave = {},
            onRetry = {},
            onPlayAgain = {},
            onOtherCategory = {},
            onSeeLeaderboard = {},
        )
    }
}
