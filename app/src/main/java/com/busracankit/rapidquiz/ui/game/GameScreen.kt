package com.busracankit.rapidquiz.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.Question
import com.busracankit.rapidquiz.ui.components.ChoiceButton
import com.busracankit.rapidquiz.ui.components.ChoiceState
import com.busracankit.rapidquiz.ui.components.CountdownRing
import com.busracankit.rapidquiz.ui.components.ErrorPanel
import com.busracankit.rapidquiz.ui.components.ProgressSegments
import com.busracankit.rapidquiz.ui.components.TertiaryButton
import com.busracankit.rapidquiz.ui.components.secondsLeft
import com.busracankit.rapidquiz.ui.theme.AccentText
import com.busracankit.rapidquiz.ui.theme.Bg
import com.busracankit.rapidquiz.ui.theme.DangerText
import com.busracankit.rapidquiz.ui.theme.InkMuted
import com.busracankit.rapidquiz.ui.theme.Primary
import com.busracankit.rapidquiz.ui.theme.QuestionTextStyle
import com.busracankit.rapidquiz.ui.theme.RapidShapes
import com.busracankit.rapidquiz.ui.theme.SpaceGrotesk
import com.busracankit.rapidquiz.ui.theme.SuccessText
import com.busracankit.rapidquiz.ui.theme.parseCategoryColor
import com.busracankit.rapidquiz.util.LocalReduceMotion
import com.busracankit.rapidquiz.util.asString
import kotlinx.coroutines.isActive
import com.busracankit.rapidquiz.ui.theme.Surface as SurfaceColor

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onFinished: () -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val phase = state.phase

    LaunchedEffect(phase) {
        if (phase == GamePhase.Finished) onFinished()
    }

    // region kesintiler
    // Arka plana gidince sayaç durur, dönünce GET /current/ ile senkronlanır (PROJE.md › 5.4).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onStart() }

    // Geri tuşu/kaydırma: "Oyundan çıkılsın mı?"
    var showQuitDialog by rememberSaveable { mutableStateOf(false) }
    val inGame = phase !is GamePhase.Finished && phase !is GamePhase.Error
    BackHandler(enabled = inGame) { showQuitDialog = true }
    if (showQuitDialog && inGame) {
        QuitDialog(
            onConfirm = {
                showQuitDialog = false
                viewModel.quit()
                onExit()
            },
            onDismiss = { showQuitDialog = false },
        )
    }
    // endregion

    Scaffold(containerColor = Bg) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (phase) {
                GamePhase.Starting, GamePhase.Syncing, GamePhase.Finished -> Loading()

                is GamePhase.Countdown -> CountdownOverlay(
                    category = state.category,
                    startsAt = phase.startsAt,
                    now = viewModel::now,
                )

                is GamePhase.Error -> Box(
                    Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorPanel(
                        title = phase.message.asString(),
                        primaryLabel = phase.retry?.let { stringResource(R.string.retry) },
                        onPrimary = viewModel::retry,
                        secondaryLabel = stringResource(R.string.back_home),
                        onSecondary = {
                            viewModel.quit()
                            onExit()
                        },
                    )
                }

                GamePhase.Playing,
                is GamePhase.Answering,
                is GamePhase.AnswerFailed,
                is GamePhase.Feedback,
                -> state.question?.let { question ->
                    GameBoard(
                        state = state,
                        question = question,
                        remainingMs = { viewModel.remainingMs() },
                        onChoice = viewModel::onChoiceSelected,
                        onRetryAnswer = viewModel::retryAnswer,
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}

/** Tam ekran 3-2-1. Kalan süre her karede monoton saatten hesaplanır (sabit 1 sn adımlarla değil). */
@Composable
private fun CountdownOverlay(category: CategoryBrief?, startsAt: Long, now: () -> Long) {
    val color = parseCategoryColor(category?.color)
    var remaining by remember(startsAt) { mutableLongStateOf(startsAt - now()) }
    LaunchedEffect(startsAt) {
        while (isActive && remaining > 0) {
            remaining = withInfiniteAnimationFrameMillis { startsAt - now() }
        }
    }
    val number = secondsLeft(remaining).coerceIn(1, 3)

    Box(
        Modifier.fillMaxSize().background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            category?.let {
                Text(it.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            }
            Text(stringResource(R.string.get_ready), style = MaterialTheme.typography.titleLarge)
            Surface(
                shape = CircleShape,
                color = SurfaceColor,
                border = BorderStroke(8.dp, color),
                modifier = Modifier.size(160.dp).semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = number,
                        transitionSpec = { (scaleIn(initialScale = 1.6f) + fadeIn()) togetherWith fadeOut() },
                        label = "countdown",
                    ) { n ->
                        Text(
                            n.toString(),
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 88.sp,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.rules_hint),
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GameBoard(
    state: GameUiState,
    question: Question,
    remainingMs: () -> Long,
    onChoice: (Int) -> Unit,
    onRetryAnswer: () -> Unit,
) {
    val phase = state.phase
    val isPlaying = phase == GamePhase.Playing
    val haptics = LocalHapticFeedback.current

    // Halka: oynarken her karede monoton saatten okunur; cevap verilince donar.
    var remaining by remember(question.id) { mutableLongStateOf(remainingMs()) }
    LaunchedEffect(question.id, isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isActive) {
            val r = withInfiniteAnimationFrameMillis { remainingMs() }
            remaining = r
            if (r <= 0) break
        }
    }

    // Son 1 saniyeye girerken bir kez hafif titreşim
    val inLastSecond = isPlaying && remaining in 1..1000
    LaunchedEffect(question.id, inLastSecond) {
        if (inLastSecond) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    // Yanlışta hafif titreşim
    LaunchedEffect(phase) {
        if (phase is GamePhase.Feedback && !phase.answer.isCorrect) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Üst satır: index/20 ve puan
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.progress, question.index, state.totalQuestions),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${stringResource(R.string.score)} ",
                style = MaterialTheme.typography.titleMedium,
                color = InkMuted,
            )
            Text(
                state.score.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = Primary,
            )
        }

        ProgressSegments(total = state.totalQuestions, currentIndex = question.index, results = state.results)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CountdownRing(remainingMs = remaining, totalMs = state.timeLimitMs)
        }

        Surface(
            shape = RapidShapes.Card,
            color = SurfaceColor,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                question.text,
                style = QuestionTextStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .heightIn(min = 64.dp)
                    .fillMaxWidth()
                    .semantics { heading() },
            )
        }

        // Şıklar A=1, B=2, C=3, D=4 sırasıyla
        val choices = question.choices.sortedBy { it.id }
        choices.forEachIndexed { i, choice ->
            ChoiceButton(
                letter = ('A' + i).toString(),
                text = choice.text,
                state = choiceState(phase, choice.id),
                enabled = isPlaying,
                onClick = { onChoice(choice.id) },
            )
        }

        FeedbackArea(phase = phase, question = question, onRetryAnswer = onRetryAnswer)
    }
}

/** Şıkkın görünümü (PROJE.md › 5.3). */
internal fun choiceState(phase: GamePhase, choiceId: Int): ChoiceState = when (phase) {
    GamePhase.Playing -> ChoiceState.Idle
    is GamePhase.Answering -> if (phase.selected == choiceId) ChoiceState.Pending else ChoiceState.Dimmed
    is GamePhase.AnswerFailed -> if (phase.selected == choiceId) ChoiceState.Pending else ChoiceState.Dimmed
    is GamePhase.Feedback -> {
        val selected = phase.answer.selectedChoiceId ?: phase.selected
        when {
            choiceId == selected && phase.answer.isCorrect -> ChoiceState.Correct
            choiceId == selected -> ChoiceState.Wrong
            choiceId == phase.answer.correctChoiceId -> ChoiceState.Reveal
            else -> ChoiceState.Dimmed
        }
    }
    else -> ChoiceState.Idle
}

@Composable
private fun FeedbackArea(phase: GamePhase, question: Question, onRetryAnswer: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            is GamePhase.Feedback -> {
                val answer = phase.answer
                val (label, color) = when {
                    answer.timedOut -> stringResource(R.string.time_up) to DangerText
                    answer.isCorrect -> stringResource(R.string.correct) to SuccessText
                    else -> stringResource(R.string.wrong) to DangerText
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.headlineSmall, color = color)
                        if (answer.points > 0) {
                            Spacer(Modifier.width(12.dp))
                            FlyingPoints(points = answer.points, key = question.id)
                        }
                    }
                    if (!answer.isCorrect) {
                        val correctText = question.choices.firstOrNull { it.id == answer.correctChoiceId }?.text
                        if (correctText != null) {
                            Text(
                                stringResource(R.string.correct_answer_was, correctText),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkMuted,
                            )
                        }
                    }
                }
            }

            is GamePhase.AnswerFailed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.error_network),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerText,
                    textAlign = TextAlign.Center,
                )
                TertiaryButton(stringResource(R.string.retry), onClick = onRetryAnswer)
            }

            else -> Unit
        }
    }
}

/** Doğru cevapta yukarı uçan "+132". Hareketi azalt açıksa sabit durur. */
@Composable
private fun FlyingPoints(points: Int, key: Any) {
    val reduceMotion = LocalReduceMotion.current
    val offset = remember(key) { Animatable(0f) }
    val alpha = remember(key) { Animatable(1f) }
    LaunchedEffect(key) {
        if (reduceMotion) return@LaunchedEffect
        offset.snapTo(12f)
        alpha.snapTo(0f)
        alpha.animateTo(1f, tween(120))
        offset.animateTo(-18f, tween(500))
    }
    Text(
        stringResource(R.string.points_plus, points),
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = AccentText,
        modifier = Modifier.graphicsLayer {
            translationY = offset.value * density
            this.alpha = alpha.value
        },
    )
}

@Composable
private fun QuitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text(stringResource(R.string.quit_title)) },
        text = { Text(stringResource(R.string.quit_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.quit_confirm), color = DangerText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.quit_cancel), color = Primary, fontWeight = FontWeight.Bold)
            }
        },
    )
}

