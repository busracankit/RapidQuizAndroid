package com.busracankit.rapidquiz

import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.NetworkQuizRepository
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.api.ApiJson
import com.busracankit.rapidquiz.data.api.createRapidQuizApi
import com.busracankit.rapidquiz.util.MonotonicClock
import com.busracankit.rapidquiz.util.SystemMonotonicClock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/** Elle bağımlılık kabı (Hilt yok). Testlerde sahte repository/saat ile kurulabilir. */
class AppContainer(
    val repository: QuizRepository,
    val clock: MonotonicClock = SystemMonotonicClock,
    val sessionHolder: GameSessionHolder = GameSessionHolder(),
) {
    companion object {
        fun create(
            baseUrl: String = BuildConfig.API_BASE_URL,
            debug: Boolean = BuildConfig.DEBUG,
        ): AppContainer {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("Accept", "application/json").build())
                }
                .apply {
                    if (debug) {
                        // Yalnızca istek satırı + durum kodu. Gövde/başlık LOGLANMAZ: session_token sızmasın.
                        addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                    }
                }
                .build()
            val api = createRapidQuizApi(baseUrl, client, ApiJson)
            return AppContainer(repository = NetworkQuizRepository(api, ApiJson))
        }
    }
}
