package com.busracankit.rapidquiz.ui.game

import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.api.ApiException
import com.busracankit.rapidquiz.data.api.ErrorCodes
import com.busracankit.rapidquiz.util.MonotonicClock
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Oyun zamanlaması (docs/PROJE.md › 5). Sanal zaman: saat = testScheduler.currentTime, delay sanal.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameTimingTest {
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
        val clock = MonotonicClock { testScheduler.currentTime }
        val vm = GameViewModel("yazilim", repo, clock, holder)
        runCurrent() // POST /sessions/ tamamlanır
        return vm
    }

    /** [ms] kadar sanal zaman ilerletir ve o ana planlanan işleri de çalıştırır. */
    private fun TestScope.advance(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun `başlangıç anı yanıt gelince bir kez hesaplanır ve 3-2-1 gösterilir`() = runTest(dispatcher) {
        val vm = createViewModel()

        val phase = vm.state.value.phase
        assertTrue(phase is GamePhase.Countdown)
        assertEquals(3000L, (phase as GamePhase.Countdown).startsAt - testScheduler.currentTime)
        assertEquals(holder.current?.session?.id, "s1")
    }

    @Test
    fun `starts_in_ms dolunca soru başlar ve sayaç 5000'den akar`() = runTest(dispatcher) {
        val vm = createViewModel()

        advance(2999)
        assertTrue(vm.state.value.phase is GamePhase.Countdown)

        advance(1)
        assertEquals(GamePhase.Playing, vm.state.value.phase)
        assertEquals(5000L, vm.remainingMs())

        advance(1200)
        assertEquals(3800L, vm.remainingMs())
    }

    @Test
    fun `süre dolunca choice_id null gönderilir`() = runTest(dispatcher) {
        createViewModel()

        advance(3000 + 4999)
        assertTrue(repo.answerCalls.isEmpty())

        advance(1)
        assertEquals(listOf(101 to null), repo.answerCalls)
    }

    @Test
    fun `geri bildirim yalnızca bir kez beklenir, sıradaki soru nextStartsAt'te gelir`() = runTest(dispatcher) {
        val vm = createViewModel()
        advance(3000)
        advance(1000)

        vm.onChoiceSelected(2)
        runCurrent()

        assertEquals(listOf(101 to 2), repo.answerCalls)
        assertTrue(vm.state.value.phase is GamePhase.Feedback)
        assertEquals(1, vm.state.value.results.size)

        // next_question.starts_in_ms = 800: tam 800 ms sonra soru 2 başlar, ek bekleme yok.
        advance(799)
        assertTrue(vm.state.value.phase is GamePhase.Feedback)
        advance(1)
        assertEquals(GamePhase.Playing, vm.state.value.phase)
        assertEquals(2, vm.state.value.question?.index)
        assertEquals(5000L, vm.remainingMs())
    }

    @Test
    fun `cevaptan sonra eski sayaç süre dolumu göndermez`() = runTest(dispatcher) {
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(1)
        runCurrent()
        // Soru 1'in 5 sn'si ve soru 2'nin başlaması geçer; soru 1 için null gitmemeli.
        advance(800 + 100)

        assertEquals(listOf(101 to 1), repo.answerCalls)
    }

    @Test
    fun `çift dokunmada tek istek atılır`() = runTest(dispatcher) {
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(1)
        vm.onChoiceSelected(3)
        runCurrent()

        assertEquals(listOf(101 to 1), repo.answerCalls)
    }

    @Test
    fun `soru başlamadan dokunma yok sayılır`() = runTest(dispatcher) {
        val vm = createViewModel()
        advance(1000)

        vm.onChoiceSelected(1)
        runCurrent()

        assertTrue(repo.answerCalls.isEmpty())
    }

    @Test
    fun `son soruda 900 ms geri bildirimden sonra oyun biter`() = runTest(dispatcher) {
        repo.answerResult = { qid, _ -> Result.success(testAnswer(index = qid - 100, finished = true)) }
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(2)
        runCurrent()
        assertTrue(vm.state.value.phase is GamePhase.Feedback)

        advance(GameViewModel.FINISH_FEEDBACK_MS - 1)
        assertTrue(vm.state.value.phase is GamePhase.Feedback)
        advance(1)
        assertEquals(GamePhase.Finished, vm.state.value.phase)
    }

    @Test
    fun `question_mismatch gelirse current ile senkronlanır`() = runTest(dispatcher) {
        repo.answerResult = { _, _ ->
            Result.failure(ApiException(409, ErrorCodes.QUESTION_MISMATCH, "Soru güncel değil."))
        }
        val vm = createViewModel()
        advance(3000)

        vm.onChoiceSelected(2)
        runCurrent()

        assertEquals(1, repo.currentCalls)
        // current: soru 1, starts_in_ms 0, remaining 4000 → hemen oynanır
        assertEquals(GamePhase.Playing, vm.state.value.phase)
        assertEquals(4000L, vm.remainingMs())
    }

    @Test
    fun `remainingFor sınırları`() {
        val q = testQuestion(1, startsInMs = 0)
        assertEquals(5000L, GameViewModel.remainingFor(q, startsAt = 1000, now = 500)) // henüz başlamadı
        assertEquals(5000L, GameViewModel.remainingFor(q, startsAt = 1000, now = 1000))
        assertEquals(1500L, GameViewModel.remainingFor(q, startsAt = 1000, now = 4500))
        assertEquals(0L, GameViewModel.remainingFor(q, startsAt = 1000, now = 9000))
    }
}
