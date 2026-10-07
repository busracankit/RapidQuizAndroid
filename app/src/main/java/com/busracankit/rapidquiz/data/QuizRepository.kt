package com.busracankit.rapidquiz.data

import com.busracankit.rapidquiz.data.api.ApiJson
import com.busracankit.rapidquiz.data.api.RapidQuizApi
import com.busracankit.rapidquiz.data.api.toAppException
import com.busracankit.rapidquiz.data.model.AnswerRequest
import com.busracankit.rapidquiz.data.model.AnswerResult
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.data.model.Leaderboard
import com.busracankit.rapidquiz.data.model.ScoreRequest
import com.busracankit.rapidquiz.data.model.ScoreSaved
import com.busracankit.rapidquiz.data.model.SessionStart
import com.busracankit.rapidquiz.data.model.SessionState
import com.busracankit.rapidquiz.data.model.StartSessionRequest
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/** Oyun oturumu. Token yalnızca bellekte tutulur; kalıcı depoya yazılmaz, loglanmaz. */
class GameSession(val id: String, val token: String) {
    override fun toString(): String = "GameSession(id=$id)"
    override fun equals(other: Any?): Boolean = other is GameSession && other.id == id && other.token == token
    override fun hashCode(): Int = 31 * id.hashCode() + token.hashCode()
}

/**
 * API çağrıları. Hatalar [com.busracankit.rapidquiz.data.api.AppException] olarak [Result] içinde döner;
 * ekranlar kararı hata koduna göre verir (docs/PROJE.md › 4.2).
 */
interface QuizRepository {
    suspend fun categories(): Result<List<Category>>
    suspend fun startSession(categorySlug: String): Result<SessionStart>
    suspend fun answer(session: GameSession, questionId: Int, choiceId: Int?): Result<AnswerResult>
    suspend fun current(session: GameSession): Result<SessionState>
    suspend fun result(session: GameSession): Result<GameResult>
    suspend fun saveScore(session: GameSession, playerName: String): Result<ScoreSaved>
    suspend fun leaderboard(categorySlug: String): Result<Leaderboard>
}

class NetworkQuizRepository(
    private val api: RapidQuizApi,
    private val json: Json = ApiJson,
) : QuizRepository {

    override suspend fun categories() = call { api.categories().sortedBy { it.order } }

    override suspend fun startSession(categorySlug: String) =
        call { api.startSession(StartSessionRequest(category = categorySlug)) }

    override suspend fun answer(session: GameSession, questionId: Int, choiceId: Int?) =
        call { api.answer(session.id, session.token, AnswerRequest(questionId, choiceId)) }

    override suspend fun current(session: GameSession) = call { api.current(session.id, session.token) }

    override suspend fun result(session: GameSession) = call { api.result(session.id, session.token) }

    override suspend fun saveScore(session: GameSession, playerName: String) =
        call { api.saveScore(session.id, session.token, ScoreRequest(playerName)) }

    override suspend fun leaderboard(categorySlug: String) = call { api.leaderboard(categorySlug) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e.toAppException(json))
        }
}
