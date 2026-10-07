package com.busracankit.rapidquiz.ui.result

import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.ActiveGame
import com.busracankit.rapidquiz.data.GameSession
import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.api.ApiException
import com.busracankit.rapidquiz.data.api.ErrorCodes
import com.busracankit.rapidquiz.data.model.AnswerResult
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.data.model.Leaderboard
import com.busracankit.rapidquiz.data.model.LeaderboardEntry
import com.busracankit.rapidquiz.data.model.ScoreSaved
import com.busracankit.rapidquiz.data.model.SessionStart
import com.busracankit.rapidquiz.data.model.SessionState
import com.busracankit.rapidquiz.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val category = CategoryBrief("yazilim", "Yazılım", "#3B82F6")
    private val holder = GameSessionHolder()
    private val repo = ResultFakeRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        holder.startGame(ActiveGame(GameSession("s1", "t1"), category))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sonuç yüklenir`() = runTest(dispatcher) {
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        assertEquals(2140, vm.state.value.result?.score)
    }

    @Test
    fun `oyun henüz bitmediyse kısa beklemeyle tekrar sorulur`() = runTest(dispatcher) {
        var calls = 0
        repo.resultResponse = {
            calls++
            if (calls < 3) {
                Result.failure(ApiException(409, ErrorCodes.SESSION_NOT_FINISHED, "Bitmedi"))
            } else {
                Result.success(repo.sampleResult)
            }
        }
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        assertEquals(3, calls)
        assertEquals(2140, vm.state.value.result?.score)
    }

    @Test
    fun `oturum yoksa oturum kaybı gösterilir`() = runTest(dispatcher) {
        holder.clear()
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        assertTrue(vm.state.value.sessionLost)
    }

    @Test
    fun `kısa isim istemcide reddedilir`() = runTest(dispatcher) {
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        vm.onNameChange("  B ")
        vm.save()
        advanceUntilIdle()

        assertEquals(UiText.Res(R.string.name_too_short), vm.state.value.nameError)
        assertTrue(repo.savedNames.isEmpty())
    }

    @Test
    fun `isim sadeleştirilip kaydedilir ve skor tablosuna geçilir`() = runTest(dispatcher) {
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        vm.onNameChange("  Büşra   C ")
        vm.save()
        advanceUntilIdle()

        assertEquals(listOf("Büşra C"), repo.savedNames)
        assertEquals(LeaderboardTarget("yazilim", highlightId = 981, myRank = null), vm.state.value.navigateTo)
        assertTrue(vm.state.value.result?.scoreSaved == true)
        assertEquals(981, holder.lastSaved?.entry?.id)

        vm.onNavigated()
        assertNull(vm.state.value.navigateTo)
    }

    @Test
    fun `sunucu ismi reddederse mesaj isim alanında gösterilir`() = runTest(dispatcher) {
        repo.saveResponse = {
            Result.failure(ApiException(400, ErrorCodes.INVALID_PLAYER_NAME, "Bu isim kullanılamaz."))
        }
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        vm.onNameChange("Deneme")
        vm.save()
        advanceUntilIdle()

        assertEquals(UiText.Raw("Bu isim kullanılamaz."), vm.state.value.nameError)
        assertNull(vm.state.value.navigateTo)
    }

    @Test
    fun `zaten kaydedildiyse skor tablosuna geçilir`() = runTest(dispatcher) {
        repo.saveResponse = {
            Result.failure(ApiException(409, ErrorCodes.SCORE_ALREADY_SAVED, "Zaten kaydedildi."))
        }
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        vm.onNameChange("Deneme")
        vm.save()
        advanceUntilIdle()

        assertEquals(LeaderboardTarget("yazilim"), vm.state.value.navigateTo)
    }

    @Test
    fun `ilk 10'a giremediyse sıra gönderilir`() = runTest(dispatcher) {
        repo.saveResponse = { Result.success(repo.sampleSaved.copy(rank = 37, inTop = false)) }
        val vm = ResultViewModel(repo, holder)
        advanceUntilIdle()

        vm.onNameChange("Deneme")
        vm.save()
        advanceUntilIdle()

        assertEquals(37, vm.state.value.navigateTo?.myRank)
    }
}

private class ResultFakeRepository : QuizRepository {
    private val category = CategoryBrief("yazilim", "Yazılım", "#3B82F6")
    val sampleResult = GameResult(
        sessionId = "s1", category = category, score = 2140, maxScore = 3000, correctCount = 16,
        totalQuestions = 20, totalTimeMs = 41230, scoreSaved = false,
    )
    val sampleSaved = ScoreSaved(
        entry = LeaderboardEntry(981, 4, "Büşra C", 2140, 16, 41230, ""),
        rank = 4,
        inTop = true,
        leaderboard = Leaderboard(category, emptyList()),
    )
    val savedNames = mutableListOf<String>()
    var resultResponse: () -> Result<GameResult> = { Result.success(sampleResult) }
    var saveResponse: () -> Result<ScoreSaved> = { Result.success(sampleSaved) }

    override suspend fun result(session: GameSession) = resultResponse()
    override suspend fun saveScore(session: GameSession, playerName: String): Result<ScoreSaved> {
        savedNames += playerName
        return saveResponse()
    }

    override suspend fun categories(): Result<List<Category>> = error("kullanılmıyor")
    override suspend fun startSession(categorySlug: String): Result<SessionStart> = error("kullanılmıyor")
    override suspend fun answer(session: GameSession, questionId: Int, choiceId: Int?): Result<AnswerResult> =
        error("kullanılmıyor")
    override suspend fun current(session: GameSession): Result<SessionState> = error("kullanılmıyor")
    override suspend fun leaderboard(categorySlug: String): Result<Leaderboard> = error("kullanılmıyor")
}
