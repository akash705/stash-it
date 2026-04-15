package com.stashed.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stashed.app.data.repository.MemoryRepository
import com.stashed.app.data.repository.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SearchUiState {
    data object Empty : SearchUiState           // no query entered yet
    data object Loading : SearchUiState
    data class Results(val items: List<SearchResult>) : SearchUiState
    data object NoResults : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MemoryRepository,
) : ViewModel() {

    val query = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<SearchUiState> = query
        .debounce(500)
        .flatMapLatest { q ->
            flow {
                if (q.isBlank()) {
                    emit(SearchUiState.Empty)
                    return@flow
                }
                emit(SearchUiState.Loading)
                val results = repository.search(q)
                emit(
                    if (results.isEmpty()) SearchUiState.NoResults
                    else SearchUiState.Results(results),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Empty)

    fun onQueryChange(text: String) {
        query.value = text
    }
}
