package com.busracankit.rapidquiz.data

import com.busracankit.rapidquiz.data.api.ApiException
import com.busracankit.rapidquiz.data.api.ErrorCodes
import com.busracankit.rapidquiz.data.api.NetworkException
import com.busracankit.rapidquiz.data.api.createRapidQuizApi
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class QuizRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: QuizRepository
    private val session = GameSession(id = "abc", token = "tok-123")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
        repository = NetworkQuizRepository(createRapidQuizApi(server.url("/").toString(), client))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    @Test
    fun `kategoriler order'a göre sıralı döner`() = runTest {
        enqueue(200, SampleJson.categories)

        val list = repository.categories().getOrThrow()

        assertEquals(listOf("yazilim", "yapay-zeka", "fizik"), list.map { it.slug })
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/categories/", request.url.encodedPath)
    }

    @Test
    fun `oyun başlatma 201 yanıtı çözülür`() = runTest {
        enqueue(201, SampleJson.sessionStart)

        val start = repository.startSession("yazilim").getOrThrow()

        assertEquals("kX9-secret", start.sessionToken)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/sessions/", request.url.encodedPath)
    }

    @Test
    fun `cevap isteği token başlığıyla doğru adrese gider`() = runTest {
        enqueue(200, SampleJson.answerWithNext)

        val answer = repository.answer(session, questionId = 412, choiceId = 2).getOrThrow()

        assertTrue(answer.isCorrect)
        val request = server.takeRequest()
        assertEquals("/api/v1/sessions/abc/answers/", request.url.encodedPath)
        assertEquals("tok-123", request.headers["X-Session-Token"])
    }

    @Test
    fun `skor tablosu category parametresiyle istenir`() = runTest {
        enqueue(200, SampleJson.leaderboard)

        repository.leaderboard("yazilim").getOrThrow()

        val request = server.takeRequest()
        assertEquals("/api/v1/leaderboard/", request.url.encodedPath)
        assertEquals("yazilim", request.url.queryParameter("category"))
    }

    @Test
    fun `4xx hata gövdesi ApiException koduna çevrilir`() = runTest {
        enqueue(400, SampleJson.error(ErrorCodes.INVALID_PLAYER_NAME, "İsim 2–20 karakter olmalı."))

        val error = repository.saveScore(session, "x").exceptionOrNull()

        assertTrue(error is ApiException)
        error as ApiException
        assertEquals(400, error.status)
        assertEquals(ErrorCodes.INVALID_PLAYER_NAME, error.code)
        assertEquals("İsim 2–20 karakter olmalı.", error.message)
    }

    @Test
    fun `410 oturum kaybı olarak işaretlenir`() = runTest {
        enqueue(410, SampleJson.error(ErrorCodes.SESSION_EXPIRED, "Oturumun süresi doldu."))

        val error = repository.current(session).exceptionOrNull() as ApiException

        assertTrue(error.isSessionLost)
    }

    @Test
    fun `JSON olmayan 500 yanıtı genel hata olur`() = runTest {
        enqueue(500, "<html>Server Error</html>")

        val error = repository.categories().exceptionOrNull() as ApiException

        assertEquals(500, error.status)
        assertEquals(ErrorCodes.UNKNOWN, error.code)
        assertTrue(error.isDeveloperError)
    }

    @Test
    fun `sunucuya ulaşılamazsa NetworkException`() = runTest {
        val deadUrl = server.url("/").toString()
        server.close()
        val repo = NetworkQuizRepository(createRapidQuizApi(deadUrl, OkHttpClient()))

        val error = repo.categories().exceptionOrNull()

        assertTrue("Beklenen NetworkException, gelen: $error", error is NetworkException)
    }
}
