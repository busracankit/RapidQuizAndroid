package com.busracankit.rapidquiz.data

/** docs/PROJE.md › 4. bölümdeki örnek yanıtlar. */
object SampleJson {
    val categories = """
        [
          { "slug": "yazilim", "name": "Yazılım", "description": "Kod", "icon": "code", "color": "#3B82F6", "order": 1 },
          { "slug": "fizik", "name": "Fizik", "description": "…", "icon": "atom", "color": "#06B6D4", "order": 5 },
          { "slug": "yapay-zeka", "name": "Yapay Zeka", "description": "…", "icon": "brain", "color": "#A855F7", "order": 2 }
        ]
    """.trimIndent()

    private val question1 = """
        {
          "index": 1, "id": 412, "text": "HTTP'de 404 durum kodu ne anlama gelir?", "difficulty": 1,
          "choices": [
            { "id": 1, "text": "Sunucu hatası" }, { "id": 2, "text": "Bulunamadı" },
            { "id": 3, "text": "Yetkisiz" }, { "id": 4, "text": "Yönlendirme" }
          ],
          "served_at": "2026-10-06T12:00:03.000000Z", "starts_in_ms": 3000, "remaining_ms": 5000
        }
    """.trimIndent()

    val sessionStart = """
        {
          "session_id": "3f6c1d9e-6a0b-4c55-9d3e-2b7f1c0a9e11",
          "session_token": "kX9-secret",
          "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
          "total_questions": 20,
          "time_limit_ms": 5000,
          "question": $question1
        }
    """.trimIndent()

    val answerWithNext = """
        {
          "is_correct": true, "timed_out": false, "correct_choice_id": 2, "selected_choice_id": 2,
          "points": 132, "score": 132, "correct_count": 1, "answered_count": 1, "finished": false,
          "next_question": { "index": 2, "id": 87, "text": "…", "difficulty": 1,
            "choices": [ { "id": 1, "text": "a" }, { "id": 2, "text": "b" }, { "id": 3, "text": "c" }, { "id": 4, "text": "d" } ],
            "served_at": "2026-10-06T12:00:05.000000Z", "starts_in_ms": 800, "remaining_ms": 5000 },
          "some_future_field": 1
        }
    """.trimIndent()

    val answerFinishedTimedOut = """
        {
          "is_correct": false, "timed_out": true, "correct_choice_id": 3, "selected_choice_id": null,
          "points": 0, "score": 2140, "correct_count": 16, "answered_count": 20, "finished": true,
          "next_question": null
        }
    """.trimIndent()

    val current = """
        {
          "session_id": "3f6c", "status": "in_progress",
          "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
          "total_questions": 20, "time_limit_ms": 5000, "score": 264, "correct_count": 2, "answered_count": 3,
          "finished": false,
          "question": $question1,
          "answers": [ { "index": 1, "is_correct": true, "timed_out": false, "points": 132, "response_ms": 1180 } ]
        }
    """.trimIndent()

    val result = """
        {
          "session_id": "3f6c",
          "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
          "score": 2140, "max_score": 3000, "correct_count": 16, "total_questions": 20, "total_time_ms": 41230,
          "finished_at": "2026-10-06T12:02:10.000000Z",
          "answers": [ { "index": 1, "is_correct": true, "timed_out": false, "points": 132, "response_ms": 1180 } ],
          "score_saved": false, "player_name": null, "rank": null
        }
    """.trimIndent()

    val scoreSaved = """
        {
          "entry": { "id": 981, "rank": 4, "player_name": "Büşra", "score": 2140, "correct_count": 16, "total_time_ms": 41230, "created_at": "2026-10-06T12:02:30.000000Z" },
          "rank": 4,
          "in_top": true,
          "leaderboard": {
            "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
            "entries": [ { "id": 12, "rank": 1, "player_name": "Ada", "score": 2710, "correct_count": 19, "total_time_ms": 30120, "created_at": "2026-10-05T18:11:02.000000Z" } ]
          }
        }
    """.trimIndent()

    val leaderboard = """
        {
          "category": { "slug": "yazilim", "name": "Yazılım", "color": "#3B82F6" },
          "entries": [
            { "id": 12, "rank": 1, "player_name": "Ada", "score": 2710, "correct_count": 19, "total_time_ms": 30120, "created_at": "2026-10-05T18:11:02.000000Z" }
          ]
        }
    """.trimIndent()

    fun error(code: String, message: String) =
        """{ "error": { "code": "$code", "message": "$message", "details": {} } }"""
}
