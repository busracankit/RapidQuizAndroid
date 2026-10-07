package com.busracankit.rapidquiz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.busracankit.rapidquiz.data.GameSessionHolder
import com.busracankit.rapidquiz.data.QuizRepository
import com.busracankit.rapidquiz.data.model.Category
import com.busracankit.rapidquiz.util.UiText
import com.busracankit.rapidquiz.util.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: UiText) : HomeUiState
    data class Content(val categories: List<Category>) : HomeUiState
}

class HomeViewModel(
    private val repository: QuizRepository,
    private val sessionHolder: GameSessionHolder,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.value = HomeUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = repository.categories().fold(
                onSuccess = { HomeUiState.Content(it) },
                onFailure = { HomeUiState.Error(it.toUiText()) },
            )
        }
    }

    /** Oyunda category_not_found alındıysa ana ekrana dönüşte kategoriler yenilenir (PROJE.md › 4.2). */
    fun onResume() {
        if (sessionHolder.categoriesStale) {
            sessionHolder.categoriesStale = false
            load()
        }
    }
}
