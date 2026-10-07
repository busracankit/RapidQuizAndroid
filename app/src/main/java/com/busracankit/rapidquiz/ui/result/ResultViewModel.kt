package com.busracankit.rapidquiz.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.busracankit.rapidquiz.R
import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.api.ApiException
import com.busracankit.rapidquiz.data.api.ErrorCodes
import com.busracankit.rapidquiz.data.model.GameResult
import com.busracankit.rapidquiz.util.PlayerName
import com.busracankit.rapidquiz.util.UiText
import com.busracankit.rapidquiz.util.toUiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Skor tablosuna geçiş isteği (tek seferlik). */
data class LeaderboardTarget(val slug: String, val highlightId: Int? = null, val myRank: Int? = null)

data class ResultUiState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val sessionLost: Boolean = false,
    val result: GameResult? = null,
    val name: String = "",
    val nameError: UiText? = null,
    val saving: Boolean = false,
    /** Kayıttan sonra skor tablosuna geçilir; ekran işledikten sonra [ResultViewModel.onNavigated] çağırır. */
    val navigateTo: LeaderboardTarget? = null,
)

class ResultViewModel(
    private val repository: QuizRepository,
    private val sessionHolder: GameSessionHolder,
) : ViewModel() {

    private val game = sessionHolder.current

    private val _state = MutableStateFlow(ResultUiState())
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    /** Oynanan kategori ("Tekrar oyna" için). */
    val categorySlug: String? get() = game?.category?.slug ?: _state.value.result?.category?.slug

    init {
        load()
    }

    fun load() {
        val activeGame = game
        if (activeGame == null) {
            // Süreç yeniden başlatıldıysa oturum bellekte yok.
            _state.update {
                it.copy(loading = false, error = UiText.Res(R.string.error_session_lost), sessionLost = true)
            }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            var attempt = 0
            while (true) {
                val response = repository.result(activeGame.session)
                val error = response.exceptionOrNull()
                // Sunucu son cevabı henüz işlemiyorsa kısa bir bekleyip tekrar sor.
                if (error is ApiException && error.code == ErrorCodes.SESSION_NOT_FINISHED && attempt < 3) {
                    attempt++
                    delay(700)
                    continue
                }
                response
                    .onSuccess { result -> _state.update { it.copy(loading = false, result = result) } }
                    .onFailure { e ->
                        _state.update {
                            it.copy(
                                loading = false,
                                error = e.toUiText(),
                                sessionLost = (e as? ApiException)?.isSessionLost == true,
                            )
                        }
                    }
                break
            }
        }
    }

    fun onNameChange(value: String) {
        // Yazarken sınırı aşmasın; boşluklar sonradan sadeleşeceği için ham değer de 20 ile sınırlı.
        if (value.length > PlayerName.MAX_LENGTH) return
        _state.update { it.copy(name = value, nameError = null) }
    }

    fun save() {
        val activeGame = game ?: return
        val st = _state.value
        if (st.saving || st.result == null || st.result.scoreSaved) return

        val name = PlayerName.normalize(st.name)
        if (name.length < PlayerName.MIN_LENGTH) {
            _state.update { it.copy(nameError = UiText.Res(R.string.name_too_short)) }
            return
        }

        _state.update { it.copy(saving = true, nameError = null, name = name) }
        viewModelScope.launch {
            repository.saveScore(activeGame.session, name)
                .onSuccess { saved ->
                    sessionHolder.lastSaved = saved
                    _state.update {
                        it.copy(
                            saving = false,
                            result = it.result?.copy(
                                scoreSaved = true,
                                playerName = saved.entry.playerName,
                                rank = saved.rank,
                            ),
                            navigateTo = LeaderboardTarget(
                                slug = activeGame.category.slug,
                                highlightId = saved.entry.id,
                                myRank = if (saved.inTop) null else saved.rank,
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    when {
                        // Zaten kaydedilmiş: kaydedilmiş say, skor tablosuna geç.
                        e is ApiException && e.code == ErrorCodes.SCORE_ALREADY_SAVED -> _state.update {
                            it.copy(
                                saving = false,
                                result = it.result?.let { r -> r.copy(scoreSaved = true, playerName = r.playerName ?: name) },
                                navigateTo = LeaderboardTarget(slug = activeGame.category.slug),
                            )
                        }

                        e is ApiException && e.isSessionLost -> _state.update {
                            it.copy(saving = false, error = e.toUiText(), sessionLost = true)
                        }

                        // invalid_player_name, rate_limited, ağ hatası…: isim alanının altında
                        else -> _state.update { it.copy(saving = false, nameError = e.toUiText()) }
                    }
                }
        }
    }

    /** "Skor tablosunu gör": kayıt yapıldıysa kullanıcının satırı vurgulanır. */
    fun leaderboardTarget(): LeaderboardTarget? {
        val slug = categorySlug ?: return null
        val saved = sessionHolder.lastSaved?.takeIf { it.leaderboard.category.slug == slug }
        return LeaderboardTarget(
            slug = slug,
            highlightId = saved?.entry?.id,
            myRank = saved?.takeIf { !it.inTop }?.rank,
        )
    }

    fun onNavigated() {
        _state.update { it.copy(navigateTo = null) }
    }
}
