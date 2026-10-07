package com.busracankit.rapidquiz.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.ActiveGame
import com.busracankit.rapidquiz.data.GameSession
import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.api.ApiException
import com.busracankit.rapidquiz.data.api.ErrorCodes
import com.busracankit.rapidquiz.data.api.NetworkException
import com.busracankit.rapidquiz.data.model.AnswerResult
import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.Question
import com.busracankit.rapidquiz.data.model.SessionState
import com.busracankit.rapidquiz.util.MonotonicClock
import com.busracankit.rapidquiz.util.UiText
import com.busracankit.rapidquiz.util.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Oyun ekranının aşamaları (docs/PROJE.md › 10.4). */
sealed interface GamePhase {
    /** POST /sessions/ bekleniyor. */
    data object Starting : GamePhase

    /** 3-2-1 (ilk soru). [startsAt] monoton saatte sorunun başlayacağı an. */
    data class Countdown(val startsAt: Long) : GamePhase

    /** Soru görünür, sayaç akıyor. Yalnızca bu aşamada cevap verilebilir. */
    data object Playing : GamePhase

    /** Cevap isteği gitti, yanıt bekleniyor. [selected] null ise süre doldu. */
    data class Answering(val selected: Int?) : GamePhase

    /** Cevap isteği ağ hatası verdi; bir kez "Tekrar dene" sunulur. */
    data class AnswerFailed(val questionId: Int, val selected: Int?) : GamePhase

    /** Doğru/yanlış gösterimi; sıradaki soru kendi başlangıç anında gelir. */
    data class Feedback(val answer: AnswerResult, val selected: Int?) : GamePhase

    /** GET /current/ ile sunucuyla eşitleniyor. */
    data object Syncing : GamePhase

    /** Oyun bitti → Sonuç ekranına geçilir. */
    data object Finished : GamePhase

    data class Error(val message: UiText, val sessionLost: Boolean, val retry: RetryAction?) : GamePhase
}

enum class RetryAction { Restart, Resync }

data class GameUiState(
    val phase: GamePhase = GamePhase.Starting,
    val category: CategoryBrief? = null,
    val question: Question? = null,
    /** Monoton saat (ms): mevcut sorunun sayacının başladığı an. */
    val questionStartsAt: Long = 0,
    val totalQuestions: Int = 20,
    val timeLimitMs: Long = 5000,
    val score: Int = 0,
    val correctCount: Int = 0,
    /** Cevaplanan soruların sonucu: soru sırası (1–20) → doğru mu. İlerleme çubuğu için. */
    val results: Map<Int, Boolean> = emptyMap(),
)

/**
 * Oyun akışı ve zamanlama (docs/PROJE.md › 5). Kurallar:
 * - Başlangıç anı yanıt geldiği anda BİR KEZ hesaplanır: startsAt = now() + starts_in_ms.
 * - Kalan süre = max(0, remaining_ms − max(0, now() − startsAt)).
 * - Geri bildirimden sonra sıradaki soru nextStartsAt'te gelir; EK BEKLEME YOK.
 * - Puan, süre, doğru cevap kararı sunucudadır.
 */
class GameViewModel(
    private val categorySlug: String,
    private val repository: QuizRepository,
    private val clock: MonotonicClock,
    private val sessionHolder: GameSessionHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var session: GameSession? = null

    /** Bekleme + geri sayım (ve son sorudaki 900 ms) işi. */
    private var timerJob: Job? = null

    /** Ağ isteği işi (başlat, cevap, current). */
    private var requestJob: Job? = null

    /** Uygulama arka plana gitti; öne gelince /current/ ile senkronlanacak. */
    private var paused = false

    init {
        start()
    }

    /** Monoton saatte şimdiki an (ekrandaki halka ve 3-2-1 için). */
    fun now(): Long = clock.now()

    /** Mevcut sorunun kalan süresi (ms). */
    fun remainingMs(now: Long = clock.now()): Long {
        val s = _state.value
        val question = s.question ?: return 0
        return remainingFor(question, s.questionStartsAt, now)
    }

    // region Akış

    fun start() {
        cancelJobs()
        session = null
        paused = false
        _state.value = GameUiState()
        requestJob = viewModelScope.launch {
            repository.startSession(categorySlug)
                .onSuccess { start ->
                    val newSession = GameSession(start.sessionId, start.sessionToken)
                    session = newSession
                    sessionHolder.startGame(ActiveGame(newSession, start.category))
                    val question = start.question
                    val startsAt = clock.now() + question.startsInMs
                    _state.update {
                        it.copy(
                            phase = GamePhase.Countdown(startsAt),
                            category = start.category,
                            question = question,
                            questionStartsAt = startsAt,
                            totalQuestions = start.totalQuestions,
                            timeLimitMs = start.timeLimitMs,
                        )
                    }
                    runQuestion(question, startsAt)
                }
                .onFailure { showError(it, RetryAction.Restart) }
        }
    }

    /** Şıkka dokunuldu. Yalnızca [GamePhase.Playing] iken işlenir (çift dokunma koruması). */
    fun onChoiceSelected(choiceId: Int) = submit(choiceId)

    /** Ağ hatasından sonra cevabı aynı question_id ile bir kez daha gönderir. */
    fun retryAnswer() {
        val currentSession = session ?: return
        val failed = _state.value.phase as? GamePhase.AnswerFailed ?: return
        val question = _state.value.question
        if (question == null || question.id != failed.questionId) {
            resync()
            return
        }
        _state.update { it.copy(phase = GamePhase.Answering(failed.selected)) }
        sendAnswer(currentSession, question, failed.selected, isRetry = true)
    }

    /** Sunucudaki duruma göre devam eder (GET /current/). */
    fun resync() {
        val currentSession = session
        if (currentSession == null) {
            start()
            return
        }
        cancelJobs()
        _state.update { it.copy(phase = GamePhase.Syncing) }
        requestJob = viewModelScope.launch {
            repository.current(currentSession)
                .onSuccess { applyServerState(it) }
                .onFailure { showError(it, RetryAction.Resync) }
        }
    }

    /** "Tekrar dene" butonu. */
    fun retry() {
        when ((_state.value.phase as? GamePhase.Error)?.retry) {
            RetryAction.Restart -> start()
            RetryAction.Resync -> resync()
            null -> Unit
        }
    }

    // endregion

    // region Kesintiler (PROJE.md › 5.4)

    /** Ekran arka plana gitti: sayaç durur. */
    fun onStop() {
        val phase = _state.value.phase
        if (phase is GamePhase.Finished || phase is GamePhase.Error) return
        paused = true
        // Başlatma isteği sürüyorsa iptal edilmez; tamamlanınca öne gelişte senkronlanır.
        if (session != null) cancelJobs()
    }

    /** Ekran öne geldi: sunucuyla senkronlan. */
    fun onStart() {
        if (!paused) return
        paused = false
        if (session != null) resync()
    }

    /** Oyundan çıkıldı. Oturum terk edilir (sunucu 30 dk sonra kendisi kapatır). */
    fun quit() {
        cancelJobs()
        session = null
        sessionHolder.clear()
    }

    // endregion

    private fun runQuestion(question: Question, startsAt: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val wait = startsAt - clock.now()
            if (wait > 0) delay(wait)
            _state.update {
                it.copy(phase = GamePhase.Playing, question = question, questionStartsAt = startsAt)
            }
            val remaining = remainingFor(question, startsAt, clock.now())
            if (remaining > 0) delay(remaining)
            timerJob = null
            submit(null) // Süre doldu: "choice_id": null
        }
    }

    private fun submit(choiceId: Int?) {
        val currentSession = session ?: return
        val st = _state.value
        if (st.phase != GamePhase.Playing) return
        val question = st.question ?: return
        timerJob?.cancel()
        timerJob = null
        _state.update { it.copy(phase = GamePhase.Answering(choiceId)) }
        sendAnswer(currentSession, question, choiceId, isRetry = false)
    }

    private fun sendAnswer(currentSession: GameSession, question: Question, choiceId: Int?, isRetry: Boolean) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            repository.answer(currentSession, question.id, choiceId)
                .onSuccess { onAnswered(question, it, choiceId) }
                .onFailure { e ->
                    when {
                        e is NetworkException && !isRetry ->
                            _state.update { it.copy(phase = GamePhase.AnswerFailed(question.id, choiceId)) }

                        e is ApiException && e.code == ErrorCodes.SESSION_FINISHED -> finish()

                        e is ApiException &&
                            (e.code == ErrorCodes.QUESTION_MISMATCH || e.code == ErrorCodes.SESSION_NOT_FINISHED) ->
                            resync()

                        else -> showError(e, RetryAction.Resync)
                    }
                }
        }
    }

    private fun onAnswered(question: Question, answer: AnswerResult, selected: Int?) {
        // Sıradaki sorunun başlangıcı yanıtın geldiği anda bir kez hesaplanır.
        val now = clock.now()
        _state.update {
            it.copy(
                phase = GamePhase.Feedback(answer, selected),
                score = answer.score,
                correctCount = answer.correctCount,
                results = it.results + (question.index to answer.isCorrect),
            )
        }
        val next = answer.nextQuestion
        when {
            answer.finished -> {
                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    delay(FINISH_FEEDBACK_MS)
                    finish()
                }
            }
            // Geri bildirim, sıradaki sorunun starts_in_ms süresi kadar görünür. Ek bekleme yok.
            next != null -> runQuestion(next, now + next.startsInMs)
            else -> resync()
        }
    }

    private fun applyServerState(server: SessionState) {
        val now = clock.now()
        _state.update {
            it.copy(
                category = server.category,
                totalQuestions = server.totalQuestions,
                timeLimitMs = server.timeLimitMs,
                score = server.score,
                correctCount = server.correctCount,
                results = server.answers.associate { a -> a.index to a.isCorrect },
            )
        }
        val question = server.question
        when {
            server.finished || server.status == SessionState.STATUS_COMPLETED -> finish()
            server.status == SessionState.STATUS_EXPIRED -> showSessionLost()
            question == null -> showError(IllegalStateException("question null"), RetryAction.Resync)
            else -> {
                val startsAt = now + question.startsInMs
                val waitingPhase = if (question.index == 1 && question.startsInMs > 0) {
                    GamePhase.Countdown(startsAt)
                } else {
                    GamePhase.Syncing
                }
                _state.update { it.copy(phase = waitingPhase, question = question, questionStartsAt = startsAt) }
                runQuestion(question, startsAt)
            }
        }
    }

    private fun finish() {
        cancelJobs()
        _state.update { it.copy(phase = GamePhase.Finished) }
    }

    private fun showError(error: Throwable, retry: RetryAction) {
        cancelJobs()
        if (error is ApiException) {
            when {
                error.isSessionLost -> {
                    showSessionLost()
                    return
                }
                error.code == ErrorCodes.SESSION_FINISHED -> {
                    finish()
                    return
                }
                error.code == ErrorCodes.CATEGORY_NOT_FOUND -> sessionHolder.categoriesStale = true
            }
        }
        // Kategori yok / soru yetersiz: tekrar denemenin anlamı yok, yalnızca ana sayfa.
        val retryAction = if (
            error is ApiException &&
            (error.code == ErrorCodes.CATEGORY_NOT_FOUND || error.code == ErrorCodes.NOT_ENOUGH_QUESTIONS)
        ) {
            null
        } else {
            retry
        }
        _state.update {
            it.copy(phase = GamePhase.Error(error.toUiText(), sessionLost = false, retry = retryAction))
        }
    }

    private fun showSessionLost() {
        cancelJobs()
        session = null
        sessionHolder.clear()
        _state.update {
            it.copy(phase = GamePhase.Error(UiText.Res(R.string.error_session_lost), sessionLost = true, retry = null))
        }
    }

    private fun cancelJobs() {
        timerJob?.cancel()
        timerJob = null
        requestJob?.cancel()
        requestJob = null
    }

    override fun onCleared() {
        cancelJobs()
    }

    companion object {
        /** Son sorudan sonra geri bildirimin görünme süresi. */
        const val FINISH_FEEDBACK_MS = 900L

        fun remainingFor(question: Question, startsAt: Long, now: Long): Long =
            (question.remainingMs - (now - startsAt).coerceAtLeast(0)).coerceAtLeast(0)
    }
}
