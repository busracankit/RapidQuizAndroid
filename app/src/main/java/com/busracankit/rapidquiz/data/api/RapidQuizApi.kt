package com.busracankit.rapidquiz.data.api

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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rapid Quiz API v1. Yollar "/" ile BAŞLAMAZ (base URL'ye eklenir) ve "/" ile BİTER
 * (Django eğik çizgisiz POST'u yönlendiremez).
 */
interface RapidQuizApi {
    @GET("api/v1/categories/")
    suspend fun categories(): List<Category>

    @POST("api/v1/sessions/")
    suspend fun startSession(@Body body: StartSessionRequest): SessionStart

    @POST("api/v1/sessions/{id}/answers/")
    suspend fun answer(
        @Path("id") id: String,
        @Header(SESSION_TOKEN_HEADER) token: String,
        @Body body: AnswerRequest,
    ): AnswerResult

    @GET("api/v1/sessions/{id}/current/")
    suspend fun current(@Path("id") id: String, @Header(SESSION_TOKEN_HEADER) token: String): SessionState

    @GET("api/v1/sessions/{id}/result/")
    suspend fun result(@Path("id") id: String, @Header(SESSION_TOKEN_HEADER) token: String): GameResult

    @POST("api/v1/sessions/{id}/score/")
    suspend fun saveScore(
        @Path("id") id: String,
        @Header(SESSION_TOKEN_HEADER) token: String,
        @Body body: ScoreRequest,
    ): ScoreSaved

    @GET("api/v1/leaderboard/")
    suspend fun leaderboard(@Query("category") slug: String): Leaderboard

    companion object {
        const val SESSION_TOKEN_HEADER = "X-Session-Token"
    }
}

/** Uygulama genelinde tek JSON ayarı. Sunucu ileride alan eklerse uygulama bozulmasın. */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    // client_type gibi varsayılanlı alanlar da gövdeye yazılsın.
    encodeDefaults = true
    // "choice_id": null açıkça yazılsın (varsayılan da budur, bilerek belirtiliyor).
    explicitNulls = true
}

/** Retrofit arayüzünü kurar. [baseUrl] "/" ile bitmeli. */
fun createRapidQuizApi(baseUrl: String, client: OkHttpClient, json: Json = ApiJson): RapidQuizApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
        .build()
        .create(RapidQuizApi::class.java)
