package com.busracankit.rapidquiz.data

import com.busracankit.rapidquiz.data.api.ApiJson
import com.busracankit.rapidquiz.data.model.AnswerRequest
import com.busracankit.rapidquiz.data.model.AnswerResult
import com.busracankit.rapidquiz.data.model.ApiErrorBody
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.data.model.Leaderboard
import com.busracankit.rapidquiz.data.model.ScoreRequest
import com.busracankit.rapidquiz.data.model.ScoreSaved
import com.busracankit.rapidquiz.data.model.SessionStart
import com.busracankit.rapidquiz.data.model.SessionState
import com.busracankit.rapidquiz.data.model.StartSessionRequest
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTest {
    private val json = ApiJson

    @Test
    fun `süre dolunca choice_id null olarak açıkça yazılır`() {
        val encoded = json.encodeToString(AnswerRequest.serializer(), AnswerRequest(1, null))
        assertEquals("""{"question_id":1,"choice_id":null}""", encoded)
    }

    @Test
    fun `cevap isteği seçilen şıkkı yazar`() {
        val encoded = json.encodeToString(AnswerRequest.serializer(), AnswerRequest(412, 2))
        assertEquals("""{"question_id":412,"choice_id":2}""", encoded)
    }

    @Test
    fun `oyun başlatma isteği client_type android gönderir`() {
        val encoded = json.encodeToString(StartSessionRequest.serializer(), StartSessionRequest("yazilim"))
        assertEquals("""{"category":"yazilim","client_type":"android"}""", encoded)
    }

    @Test
    fun `skor isteği player_name yazar`() {
        val encoded = json.encodeToString(ScoreRequest.serializer(), ScoreRequest("Büşra"))
        assertEquals("""{"player_name":"Büşra"}""", encoded)
    }

    @Test
    fun `kategoriler çözülür`() {
        val list = json.decodeFromString(ListSerializer(Category.serializer()), SampleJson.categories)
        assertEquals(3, list.size)
        assertEquals("yazilim", list[0].slug)
        assertEquals("#3B82F6", list[0].color)
        assertEquals("code", list[0].icon)
    }

    @Test
    fun `oyun başlatma yanıtı çözülür`() {
        val start = json.decodeFromString(SessionStart.serializer(), SampleJson.sessionStart)
        assertEquals("3f6c1d9e-6a0b-4c55-9d3e-2b7f1c0a9e11", start.sessionId)
        assertEquals(20, start.totalQuestions)
        assertEquals(5000L, start.timeLimitMs)
        assertEquals(1, start.question.index)
        assertEquals(412, start.question.id)
        assertEquals(4, start.question.choices.size)
        assertEquals(3000L, start.question.startsInMs)
        assertEquals(5000L, start.question.remainingMs)
        // Token toString'e yazılmaz
        assertFalse(start.toString().contains("kX9"))
    }

    @Test
    fun `cevap yanıtı sıradaki soruyla çözülür, bilinmeyen alanlar yok sayılır`() {
        val answer = json.decodeFromString(AnswerResult.serializer(), SampleJson.answerWithNext)
        assertTrue(answer.isCorrect)
        assertEquals(132, answer.points)
        assertEquals(2, answer.nextQuestion?.index)
        assertEquals(800L, answer.nextQuestion?.startsInMs)
    }

    @Test
    fun `son cevap yanıtında next_question null`() {
        val answer = json.decodeFromString(AnswerResult.serializer(), SampleJson.answerFinishedTimedOut)
        assertTrue(answer.finished)
        assertTrue(answer.timedOut)
        assertNull(answer.selectedChoiceId)
        assertNull(answer.nextQuestion)
    }

    @Test
    fun `oyun durumu çözülür`() {
        val state = json.decodeFromString(SessionState.serializer(), SampleJson.current)
        assertEquals(SessionState.STATUS_IN_PROGRESS, state.status)
        assertEquals(264, state.score)
        assertEquals(1, state.answers.size)
        assertEquals(1180L, state.answers[0].responseMs)
    }

    @Test
    fun `sonuç çözülür`() {
        val result = json.decodeFromString(GameResult.serializer(), SampleJson.result)
        assertEquals(2140, result.score)
        assertEquals(41230L, result.totalTimeMs)
        assertFalse(result.scoreSaved)
        assertNull(result.playerName)
    }

    @Test
    fun `skor kaydı ve skor tablosu çözülür`() {
        val saved = json.decodeFromString(ScoreSaved.serializer(), SampleJson.scoreSaved)
        assertEquals(981, saved.entry.id)
        assertTrue(saved.inTop)
        assertEquals("Ada", saved.leaderboard.entries[0].playerName)

        val board = json.decodeFromString(Leaderboard.serializer(), SampleJson.leaderboard)
        assertEquals("yazilim", board.category.slug)
        assertEquals(1, board.entries.size)
    }

    @Test
    fun `hata gövdesi çözülür`() {
        val body = json.decodeFromString(
            ApiErrorBody.serializer(),
            SampleJson.error("invalid_player_name", "İsim 2–20 karakter olmalı."),
        )
        assertEquals("invalid_player_name", body.error.code)
        assertEquals("İsim 2–20 karakter olmalı.", body.error.message)
    }
}
