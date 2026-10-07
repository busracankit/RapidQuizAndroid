package com.busracankit.rapidquiz.ui.game

import com.busracankit.rapidquiz.data.GameSession
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.model.AnswerResult
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.Choice
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.data.model.Leaderboard
import com.busracankit.rapidquiz.data.model.Question
import com.busracankit.rapidquiz.data.model.ScoreSaved
import com.busracankit.rapidquiz.data.model.SessionStart
import com.busracankit.rapidquiz.data.model.SessionState

val testCategory = CategoryBrief("yazilim", "Yazılım", "#3B82F6")

fun testQuestion(index: Int, startsInMs: Long, remainingMs: Long = 5000) = Question(
    index = index,
    id = 100 + index,
    text = "Soru $index",
    difficulty = 1,
    choices = (1..4).map { Choice(it, "Şık $it") },
    servedAt = "",
    startsInMs = startsInMs,
    remainingMs = remainingMs,
)

fun testAnswer(
    index: Int,
    correct: Boolean = true,
    timedOut: Boolean = false,
    finished: Boolean = false,
    next: Question? = if (finished) null else testQuestion(index + 1, startsInMs = 800),
) = AnswerResult(
    isCorrect = correct,
    timedOut = timedOut,
    correctChoiceId = 2,
    selectedChoiceId = null,
    points = if (correct) 120 else 0,
    score = 120 * index,
    correctCount = index,
    answeredCount = index,
    finished = finished,
    nextQuestion = next,
)

/** Sahte repository: yanıtlar anında (sanal zamanda 0 ms) döner, çağrılar kaydedilir. */
class FakeQuizRepository : QuizRepository {
    val answerCalls = mutableListOf<Pair<Int, Int?>>()
    var currentCalls = 0
    var startCalls = 0

    var startResult: () -> Result<SessionStart> = {
        Result.success(
            SessionStart(
                sessionId = "s1",
                sessionToken = "t1",
                category = testCategory,
                totalQuestions = 20,
                timeLimitMs = 5000,
                question = testQuestion(1, startsInMs = 3000),
            ),
        )
    }

    /** (questionId, choiceId) → yanıt. Varsayılan: doğru, sıradaki soru 800 ms sonra. */
    var answerResult: (Int, Int?) -> Result<AnswerResult> = { questionId, _ ->
        Result.success(testAnswer(index = questionId - 100))
    }

    var currentResult: () -> Result<SessionState> = {
        Result.success(
            SessionState(
                sessionId = "s1",
                status = SessionState.STATUS_IN_PROGRESS,
                category = testCategory,
                totalQuestions = 20,
                timeLimitMs = 5000,
                score = 0,
                correctCount = 0,
                answeredCount = 0,
                finished = false,
                question = testQuestion(1, startsInMs = 0, remainingMs = 4000),
            ),
        )
    }

    override suspend fun categories(): Result<List<Category>> = Result.success(emptyList())

    override suspend fun startSession(categorySlug: String): Result<SessionStart> {
        startCalls++
        return startResult()
    }

    override suspend fun answer(session: GameSession, questionId: Int, choiceId: Int?): Result<AnswerResult> {
        answerCalls += questionId to choiceId
        return answerResult(questionId, choiceId)
    }

    override suspend fun current(session: GameSession): Result<SessionState> {
        currentCalls++
        return currentResult()
    }

    override suspend fun result(session: GameSession): Result<GameResult> = error("kullanılmıyor")
    override suspend fun saveScore(session: GameSession, playerName: String): Result<ScoreSaved> = error("kullanılmıyor")
    override suspend fun leaderboard(categorySlug: String): Result<Leaderboard> = error("kullanılmıyor")
}
