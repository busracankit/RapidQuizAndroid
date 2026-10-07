package com.busracankit.rapidquiz.data.api

import com.busracankit.rapidquiz.data.model.ApiErrorBody
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Repository'den dönen bütün hatalar bu tiplerden biridir. */
sealed class AppException(message: String?, cause: Throwable? = null) : Exception(message, cause)

/** Sunucu hata gövdesi: { "error": { "code", "message", "details" } }. [message] Türkçe, gösterilebilir. */
class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
) : AppException(message) {
    /** "Oturum bulunamadı ya da süresi doldu" → ana sayfa. */
    val isSessionLost: Boolean
        get() = code in ErrorCodes.SESSION_LOST || status == 403 || status == 410

    /** Geliştirme hatası: kullanıcıya genel mesaj gösterilir. */
    val isDeveloperError: Boolean
        get() = code in ErrorCodes.DEVELOPER || status >= 500
}

/** Bağlantı yok / zaman aşımı. */
class NetworkException(cause: Throwable) : AppException(cause.message, cause)

/** Beklenmeyen durum (ör. çözülemeyen yanıt). */
class UnexpectedException(cause: Throwable) : AppException(cause.message, cause)

/** docs/PROJE.md › 4.2 */
object ErrorCodes {
    const val VALIDATION_ERROR = "validation_error"
    const val INVALID_JSON = "invalid_json"
    const val INVALID_CHOICE = "invalid_choice"
    const val INVALID_PLAYER_NAME = "invalid_player_name"
    const val INVALID_SESSION_TOKEN = "invalid_session_token"
    const val CATEGORY_NOT_FOUND = "category_not_found"
    const val SESSION_NOT_FOUND = "session_not_found"
    const val NOT_FOUND = "not_found"
    const val NOT_ENOUGH_QUESTIONS = "not_enough_questions"
    const val QUESTION_MISMATCH = "question_mismatch"
    const val SESSION_FINISHED = "session_finished"
    const val SESSION_NOT_FINISHED = "session_not_finished"
    const val SCORE_ALREADY_SAVED = "score_already_saved"
    const val SESSION_EXPIRED = "session_expired"
    const val RATE_LIMITED = "rate_limited"
    const val SERVER_ERROR = "server_error"

    /** Gövde çözülemediğinde. */
    const val UNKNOWN = "unknown"

    val SESSION_LOST = setOf(INVALID_SESSION_TOKEN, SESSION_NOT_FOUND, SESSION_EXPIRED)
    val DEVELOPER = setOf(VALIDATION_ERROR, INVALID_JSON, INVALID_CHOICE, NOT_FOUND, SERVER_ERROR, UNKNOWN)
}

/** Retrofit/OkHttp hatalarını uygulama hatalarına çevirir. */
fun Throwable.toAppException(json: Json = ApiJson): AppException = when (this) {
    is AppException -> this
    is HttpException -> toApiException(json)
    is IOException -> NetworkException(this)
    else -> UnexpectedException(this)
}

private fun HttpException.toApiException(json: Json): ApiException {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val detail = raw?.let { body -> runCatching { json.decodeFromString(ApiErrorBody.serializer(), body).error }.getOrNull() }
    return ApiException(
        status = code(),
        code = detail?.code ?: ErrorCodes.UNKNOWN,
        message = detail?.message.orEmpty(),
    )
}
