package com.busracankit.rapidquiz.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// API v1 veri sınıfları (docs/PROJE.md › 4 ve 10.2). JSON anahtarları snake_case.

@Serializable
data class Category(
    val slug: String,
    val name: String,
    val description: String = "",
    val icon: String = "",
    val color: String,
    val order: Int = 0,
)

@Serializable
data class CategoryBrief(val slug: String, val name: String, val color: String)

@Serializable
data class Choice(val id: Int, val text: String)

@Serializable
data class Question(
    val index: Int,
    val id: Int,
    val text: String,
    val difficulty: Int = 1,
    val choices: List<Choice>,
    @SerialName("served_at") val servedAt: String = "",
    @SerialName("starts_in_ms") val startsInMs: Long,
    @SerialName("remaining_ms") val remainingMs: Long,
)

@Serializable
data class StartSessionRequest(
    val category: String,
    @SerialName("client_type") val clientType: String = CLIENT_TYPE,
) {
    companion object {
        const val CLIENT_TYPE = "android"
    }
}

@Serializable
data class SessionStart(
    @SerialName("session_id") val sessionId: String,
    @SerialName("session_token") val sessionToken: String,
    val category: CategoryBrief,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("time_limit_ms") val timeLimitMs: Long,
    val question: Question,
) {
    // Token loglara/hata raporlarına sızmasın.
    override fun toString(): String = "SessionStart(sessionId=$sessionId, category=${category.slug})"
}

/**
 * DİKKAT: choiceId'ye varsayılan değer VERME. Süre dolunca "choice_id": null açıkça gönderilmeli,
 * alan hiç yoksa sunucu 400 validation_error döner.
 */
@Serializable
data class AnswerRequest(
    @SerialName("question_id") val questionId: Int,
    @SerialName("choice_id") val choiceId: Int?,
)

@Serializable
data class AnswerResult(
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("timed_out") val timedOut: Boolean,
    @SerialName("correct_choice_id") val correctChoiceId: Int,
    @SerialName("selected_choice_id") val selectedChoiceId: Int? = null,
    val points: Int,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("answered_count") val answeredCount: Int,
    val finished: Boolean,
    @SerialName("next_question") val nextQuestion: Question? = null,
)

@Serializable
data class AnswerSummary(
    val index: Int,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("timed_out") val timedOut: Boolean,
    val points: Int,
    @SerialName("response_ms") val responseMs: Long? = null,
)

@Serializable
data class SessionState(
    @SerialName("session_id") val sessionId: String,
    val status: String,
    val category: CategoryBrief,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("time_limit_ms") val timeLimitMs: Long,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("answered_count") val answeredCount: Int,
    val finished: Boolean,
    val question: Question? = null,
    val answers: List<AnswerSummary> = emptyList(),
) {
    companion object {
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_EXPIRED = "expired"
    }
}

@Serializable
data class GameResult(
    @SerialName("session_id") val sessionId: String,
    val category: CategoryBrief,
    val score: Int,
    @SerialName("max_score") val maxScore: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("total_time_ms") val totalTimeMs: Long,
    @SerialName("finished_at") val finishedAt: String? = null,
    val answers: List<AnswerSummary> = emptyList(),
    @SerialName("score_saved") val scoreSaved: Boolean,
    @SerialName("player_name") val playerName: String? = null,
    val rank: Int? = null,
)

@Serializable
data class ScoreRequest(@SerialName("player_name") val playerName: String)

@Serializable
data class LeaderboardEntry(
    val id: Int,
    val rank: Int,
    @SerialName("player_name") val playerName: String,
    val score: Int,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_time_ms") val totalTimeMs: Long,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class Leaderboard(val category: CategoryBrief, val entries: List<LeaderboardEntry>)

@Serializable
data class ScoreSaved(
    val entry: LeaderboardEntry,
    val rank: Int,
    @SerialName("in_top") val inTop: Boolean,
    val leaderboard: Leaderboard,
)

@Serializable
data class ApiErrorBody(val error: ApiErrorDetail)

@Serializable
data class ApiErrorDetail(val code: String, val message: String = "")
