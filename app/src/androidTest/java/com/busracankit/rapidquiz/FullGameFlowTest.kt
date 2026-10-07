package com.busracankit.rapidquiz

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.busracankit.rapidquiz.data.GameSession
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.model.AnswerResult
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.Choice
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.data.model.Leaderboard
import com.busracankit.rapidquiz.data.model.LeaderboardEntry
import com.busracankit.rapidquiz.data.model.Question
import com.busracankit.rapidquiz.data.model.ScoreSaved
import com.busracankit.rapidquiz.data.model.SessionStart
import com.busracankit.rapidquiz.data.model.SessionState
import com.busracankit.rapidquiz.ui.navigation.RapidQuizNavHost
import com.busracankit.rapidquiz.ui.theme.RapidQuizTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Ana ekran → kategori → 20 soru → isim → skor tablosu (sahte repository ile, docs/PROJE.md › 11).
 * Sunucu gerekmez; zamanlama gerçek saatle ama bekleme süreleri 0 verilerek hızlı çalışır.
 */
@RunWith(AndroidJUnit4::class)
class FullGameFlowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tamOyunAkisi() {
        val repo = UiFakeRepository()
        rule.setContent {
            RapidQuizTheme(reduceMotion = true) {
                RapidQuizNavHost(AppContainer(repository = repo))
            }
        }

        // 1) Kategoriler
        rule.waitUntil(TIMEOUT) { rule.onAllNodes(hasText("Yazılım")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Yazılım").performClick()

        // 2) 20 soru: her soruda A şıkkı
        val choiceA = hasContentDescription("A şıkkı: Şık 1") and isEnabled()
        for (index in 1..20) {
            // Doğru soruda olduğumuzdan emin ol (bir önceki sorunun düğümüne iki kez basılmasın)
            rule.waitUntil(TIMEOUT) {
                rule.onAllNodes(hasText("$index/20")).fetchSemanticsNodes().isNotEmpty() &&
                    rule.onAllNodes(choiceA).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNode(choiceA).performClick()
        }

        // 3) Sonuç + isim
        rule.waitUntil(TIMEOUT) { rule.onAllNodes(hasText("Oyun bitti!")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(20, repo.answered)
        rule.onNode(hasSetTextAction()).performTextInput("Büşra")
        rule.onNodeWithText("Kaydet").performClick()

        // 4) Skor tablosu: kullanıcının satırı "Sen" etiketiyle
        rule.waitUntil(TIMEOUT) { rule.onAllNodes(hasText("Sen")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("İlk 10").assertExists()
        assertEquals("Büşra", repo.savedName)
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}

private class UiFakeRepository : QuizRepository {
    private val category = CategoryBrief("yazilim", "Yazılım", "#3B82F6")
    var answered = 0
    var savedName: String? = null

    private fun question(index: Int) = Question(
        index = index,
        id = 100 + index,
        text = "Soru $index",
        difficulty = 1,
        choices = (1..4).map { Choice(it, "Şık $it") },
        servedAt = "",
        startsInMs = 0, // 3-2-1 ve geri bildirim beklemesi yok: test hızlı
        remainingMs = 5000,
    )

    override suspend fun categories() =
        Result.success(listOf(Category("yazilim", "Yazılım", "Kod soruları", "code", "#3B82F6", 1)))

    override suspend fun startSession(categorySlug: String) = Result.success(
        SessionStart("s1", "t1", category, totalQuestions = 20, timeLimitMs = 5000, question = question(1)),
    )

    override suspend fun answer(session: GameSession, questionId: Int, choiceId: Int?): Result<AnswerResult> {
        answered++
        val index = questionId - 100
        val finished = index == 20
        return Result.success(
            AnswerResult(
                isCorrect = choiceId == 1,
                timedOut = choiceId == null,
                correctChoiceId = 1,
                selectedChoiceId = choiceId,
                points = 120,
                score = 120 * index,
                correctCount = index,
                answeredCount = index,
                finished = finished,
                nextQuestion = if (finished) null else question(index + 1),
            ),
        )
    }

    override suspend fun current(session: GameSession): Result<SessionState> =
        Result.failure(IllegalStateException("beklenmiyor"))

    override suspend fun result(session: GameSession) = Result.success(
        GameResult(
            sessionId = "s1", category = category, score = 2400, maxScore = 3000, correctCount = 20,
            totalQuestions = 20, totalTimeMs = 30000, scoreSaved = false,
        ),
    )

    override suspend fun saveScore(session: GameSession, playerName: String): Result<ScoreSaved> {
        savedName = playerName
        val me = LeaderboardEntry(99, 1, playerName, 2400, 20, 30000, "")
        return Result.success(ScoreSaved(me, 1, true, Leaderboard(category, listOf(me))))
    }

    override suspend fun leaderboard(categorySlug: String) =
        Result.success(Leaderboard(category, emptyList()))
}
