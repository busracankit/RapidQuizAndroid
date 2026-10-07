package com.busracankit.rapidquiz.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.data.model.LeaderboardEntry
import com.busracankit.rapidquiz.util.UiText
import com.busracankit.rapidquiz.util.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BoardState {
    data object Loading : BoardState
    data class Error(val message: UiText) : BoardState
    data class Content(val entries: List<LeaderboardEntry>) : BoardState
}

data class LeaderboardUiState(
    val categories: List<Category> = emptyList(),
    val categoriesLoading: Boolean = true,
    val categoriesError: UiText? = null,
    val selectedSlug: String? = null,
    val board: BoardState = BoardState.Loading,
    /** Oyundan gelindiyse ve oynanan kategori seçiliyse kullanıcının satırı. */
    val highlightId: Int? = null,
    /** İlk 10'a giremediyse "Senin sıran: N." */
    val myRank: Int? = null,
)

/**
 * Skor tablosu: kategori seçici + seçili kategorinin İlk 10'u (genel liste API'de yok).
 * Ana ekrandan girilirse ilk kategori, sonuçtan girilirse oynanan kategori seçilidir.
 */
class LeaderboardViewModel(
    private val repository: QuizRepository,
    private val sessionHolder: GameSessionHolder,
    initialSlug: String?,
    private val highlightId: Int?,
    private val myRank: Int?,
) : ViewModel() {

    /** Vurgunun geçerli olduğu kategori (oyundan gelindiyse). */
    private val originSlug: String? = initialSlug.takeIf { highlightId != null || myRank != null }

    private val _state = MutableStateFlow(LeaderboardUiState(selectedSlug = initialSlug))
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    private var boardJob: Job? = null

    init {
        loadCategories()
        initialSlug?.let { loadBoard(it, allowCached = true) }
    }

    fun loadCategories() {
        _state.update { it.copy(categoriesLoading = true, categoriesError = null) }
        viewModelScope.launch {
            repository.categories()
                .onSuccess { categories ->
                    _state.update { it.copy(categories = categories, categoriesLoading = false) }
                    if (_state.value.selectedSlug == null) categories.firstOrNull()?.let { select(it.slug) }
                }
                .onFailure { e ->
                    _state.update { it.copy(categoriesLoading = false, categoriesError = e.toUiText()) }
                }
        }
    }

    fun select(slug: String) {
        // Zaten seçili ve yüklü/yükleniyorsa tekrar istek atma.
        if (slug == _state.value.selectedSlug && _state.value.board !is BoardState.Error) return
        _state.update { it.copy(selectedSlug = slug) }
        loadBoard(slug, allowCached = false)
    }

    fun retry() {
        if (_state.value.categoriesError != null) loadCategories()
        _state.value.selectedSlug?.let { loadBoard(it, allowCached = false) }
    }

    private fun loadBoard(slug: String, allowCached: Boolean) {
        boardJob?.cancel()
        val isOrigin = slug == originSlug
        val highlight = if (isOrigin) highlightId else null
        val rank = if (isOrigin) myRank else null

        // Kayıt yanıtındaki güncel İlk 10 varsa tekrar istek atılmaz.
        val cached = sessionHolder.lastSaved
            ?.takeIf { allowCached && isOrigin && it.leaderboard.category.slug == slug && it.entry.id == highlightId }
        if (cached != null) {
            _state.update {
                it.copy(board = BoardState.Content(cached.leaderboard.entries), highlightId = highlight, myRank = rank)
            }
            return
        }

        _state.update { it.copy(board = BoardState.Loading, highlightId = highlight, myRank = rank) }
        boardJob = viewModelScope.launch {
            repository.leaderboard(slug)
                .onSuccess { board ->
                    if (_state.value.selectedSlug == slug) {
                        _state.update { it.copy(board = BoardState.Content(board.entries)) }
                    }
                }
                .onFailure { e ->
                    if (_state.value.selectedSlug == slug) {
                        _state.update { it.copy(board = BoardState.Error(e.toUiText())) }
                    }
                }
        }
    }
}
