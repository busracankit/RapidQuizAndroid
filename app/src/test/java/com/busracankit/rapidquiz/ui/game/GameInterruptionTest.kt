package com.busracankit.rapidquiz.ui.game

import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.api.ApiException
import com.busracankit.rapidquiz.data.api.ErrorCodes
import com.busracankit.rapidquiz.data.api.NetworkException
import com.busracankit.rapidquiz.data.model.SessionState
import com.busracankit.rapidquiz.util.MonotonicClock
import com.busracankit.rapidquiz.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Arka plan, ağ hatası, oturum kaybı (docs/PROJE.md › 5.4). */
@OptIn(ExperimentalCoroutinesApi::class)
class GameInterruptionTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeQuizRepository
    private lateinit var holder: GameSessionHolder

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeQuizRepository()
        holder = GameSessionHolder()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(): GameViewModel {
        val vm = GameViewModel("yazilim", repo, MonotonicClock { testScheduler.currentTime }, holder)
        runCurrent()
        return vm
    }

    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun `arka plandayken sayaç durur, dönünce current ile senkronlanır`() = runTest(dispatcher) {
        val vm = createViewModel()
        advance(3000)

        vm.onStop()
        advance(10_000) // arka planda 10 sn
        assertTrue("Arka planda süre dolumu gönderilmemeli", repo.answerCalls.isEmpty())

        vm.onStart()
        runCurrent()
        assertEquals(1, repo.currentCalls)
        assertEquals(GamePhase.Playing, vm.state.value.phase)
    }

    @Test
    fun `senkronda oyun bitmişse sonuca geçilir`() = runTest(dispatcher) {
        repo.currentResult = {
            Result.success(
                SessionState(
                    sessionId = "s1", status = SessionState.STATUS_COMPLETED, category = testCategory,
                    totalQuestions = 20, timeLimitMs = 5000, score = 900, correctCount = 8, answeredCount = 20,
                    finished = true, question = null,
                ),
            )
        }
        val vm = createViewModel()
        advance(3000)

        vm.onStop()
        vm.onStart()
        runCurrent()

        assertEquals(GamePhase.Finished, vm.state.value.phase)
    }

    @Test
    fun `cevap ağ hatasında bir kez tekrar denenir, aynı question_id gider`() = runTest(dispatcher) {
        var calls = 0
        repo.answerResult = { qid, _ ->
            calls++
            if (calls == 1) Result.failure(NetworkException(IOException("yok"))) else Result.success(testAnswer(qid - 100))
        }
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(3)
        runCurrent()
        assertTrue(vm.state.value.phase is GamePhase.AnswerFailed)

        vm.retryAnswer()
        runCurrent()

        assertEquals(listOf(101 to 3, 101 to 3), repo.answerCalls)
        assertTrue(vm.state.value.phase is GamePhase.Feedback)
    }

    @Test
    fun `tekrar deneme de ağ hatası verirse hata ekranı ve senkron seçeneği`() = runTest(dispatcher) {
        repo.answerResult = { _, _ -> Result.failure(NetworkException(IOException("yok"))) }
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(3)
        runCurrent()
        vm.retryAnswer()
        runCurrent()

        val phase = vm.state.value.phase as GamePhase.Error
        assertEquals(RetryAction.Resync, phase.retry)
        assertEquals(UiText.Res(R.string.error_network), phase.message)
    }

    @Test
    fun `410 oturum kaybında ana sayfaya dönüş hatası`() = runTest(dispatcher) {
        repo.answerResult = { _, _ ->
            Result.failure(ApiException(410, ErrorCodes.SESSION_EXPIRED, "Oturumun süresi doldu."))
        }
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(1)
        runCurrent()

        val phase = vm.state.value.phase as GamePhase.Error
        assertTrue(phase.sessionLost)
        assertNull(phase.retry)
        assertNull(holder.current)
    }

    @Test
    fun `session_finished gelirse sonuca geçilir`() = runTest(dispatcher) {
        repo.answerResult = { _, _ ->
            Result.failure(ApiException(409, ErrorCodes.SESSION_FINISHED, "Oyun bitti."))
        }
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(1)
        runCurrent()

        assertEquals(GamePhase.Finished, vm.state.value.phase)
    }

    @Test
    fun `başlatma hatasında tekrar dene yeni oyun ister`() = runTest(dispatcher) {
        var fail = true
        val ok = repo.startResult
        repo.startResult = { if (fail) Result.failure(NetworkException(IOException("yok"))) else ok() }
        val vm = createViewModel()

        val phase = vm.state.value.phase as GamePhase.Error
        assertEquals(RetryAction.Restart, phase.retry)

        fail = false
        vm.retry()
        runCurrent()
        assertEquals(2, repo.startCalls)
        assertTrue(vm.state.value.phase is GamePhase.Countdown)
    }

    @Test
    fun `kategori yoksa tekrar dene sunulmaz ve kategoriler yenilenir`() = runTest(dispatcher) {
        repo.startResult = {
            Result.failure(ApiException(404, ErrorCodes.CATEGORY_NOT_FOUND, "Kategori bulunamadı."))
        }
        val vm = createViewModel()

        val phase = vm.state.value.phase as GamePhase.Error
        assertNull(phase.retry)
        assertEquals(UiText.Raw("Kategori bulunamadı."), phase.message)
        assertTrue(holder.categoriesStale)
    }

    @Test
    fun `oyundan çıkınca oturum bırakılır ve sayaç durur`() = runTest(dispatcher) {
        val vm = createViewModel()
        advance(3000)

        vm.quit()
        advance(10_000)

        assertTrue(repo.answerCalls.isEmpty())
        assertNull(holder.current)
    }
}
