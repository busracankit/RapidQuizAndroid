package com.busracankit.rapidquiz.ui.leaderboard

import com.busracankit.rapidquiz.data.GameSession
import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.model.AnswerResult
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.CategoryBrief
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.data.model.Leaderboard
import com.busracankit.rapidquiz.data.model.LeaderboardEntry
import com.busracankit.rapidquiz.data.model.ScoreSaved
import com.busracankit.rapidquiz.data.model.SessionStart
import com.busracankit.rapidquiz.data.model.SessionState
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
class LeaderboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = BoardFakeRepository()
    private val holder = GameSessionHolder()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `ana ekrandan girilince ilk kategori seçilir`() = runTest(dispatcher) {
        val vm = LeaderboardViewModel(repo, holder, initialSlug = null, highlightId = null, myRank = null)
        advanceUntilIdle()

        assertEquals("yazilim", vm.state.value.selectedSlug)
        assertEquals(listOf("yazilim"), repo.boardRequests)
        assertTrue(vm.state.value.board is BoardState.Content)
        assertNull(vm.state.value.highlightId)
    }

    @Test
    fun `sonuçtan gelince oynanan kategori seçili ve satır vurgulu, kayıt yanıtı kullanılır`() = runTest(dispatcher) {
        val entry = LeaderboardEntry(981, 4, "Büşra", 2140, 16, 41230, "")
        val category = CategoryBrief("fizik", "Fizik", "#06B6D4")
        holder.lastSaved = ScoreSaved(entry, 4, true, Leaderboard(category, listOf(entry)))

        val vm = LeaderboardViewModel(repo, holder, initialSlug = "fizik", highlightId = 981, myRank = null)
        advanceUntilIdle()

        assertEquals("fizik", vm.state.value.selectedSlug)
        assertEquals(981, vm.state.value.highlightId)
        assertTrue("Kayıt yanıtındaki liste kullanılmalı", repo.boardRequests.isEmpty())
    }

    @Test
    fun `başka kategoriye geçince vurgu ve sıra gösterilmez`() = runTest(dispatcher) {
        val vm = LeaderboardViewModel(repo, holder, initialSlug = "fizik", highlightId = 981, myRank = 37)
        advanceUntilIdle()
        assertEquals(37, vm.state.value.myRank)

        vm.select("yazilim")
        advanceUntilIdle()

        assertNull(vm.state.value.highlightId)
        assertNull(vm.state.value.myRank)
        assertEquals(listOf("fizik", "yazilim"), repo.boardRequests)
    }

    @Test
    fun `aynı kategoriye tekrar dokunmak istek atmaz`() = runTest(dispatcher) {
        val vm = LeaderboardViewModel(repo, holder, initialSlug = "fizik", highlightId = null, myRank = null)
        advanceUntilIdle()

        vm.select("fizik")
        advanceUntilIdle()

        assertEquals(listOf("fizik"), repo.boardRequests)
    }
}

private class BoardFakeRepository : QuizRepository {
    val boardRequests = mutableListOf<String>()

    override suspend fun categories(): Result<List<Category>> = Result.success(
        listOf(
            Category("yazilim", "Yazılım", "", "code", "#3B82F6", 1),
            Category("fizik", "Fizik", "", "atom", "#06B6D4", 5),
        ),
    )

    override suspend fun leaderboard(categorySlug: String): Result<Leaderboard> {
        boardRequests += categorySlug
        return Result.success(
            Leaderboard(
                CategoryBrief(categorySlug, categorySlug, "#3B82F6"),
                listOf(LeaderboardEntry(12, 1, "Ada", 2710, 19, 30120, "")),
            ),
        )
    }

    override suspend fun startSession(categorySlug: String): Result<SessionStart> = error("kullanılmıyor")
    override suspend fun answer(session: GameSession, questionId: Int, choiceId: Int?): Result<AnswerResult> =
        error("kullanılmıyor")
    override suspend fun current(session: GameSession): Result<SessionState> = error("kullanılmıyor")
    override suspend fun result(session: GameSession): Result<GameResult> = error("kullanılmıyor")
    override suspend fun saveScore(session: GameSession, playerName: String): Result<ScoreSaved> = error("kullanılmıyor")
}
