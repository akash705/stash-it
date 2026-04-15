package com.stashed.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stashed.app.data.local.LocationHistoryEntity
import com.stashed.app.data.local.MemoryEntity
import com.stashed.app.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ListUiState {
    data object Loading : ListUiState
    data object Empty : ListUiState
    data class Loaded(val memories: List<MemoryEntity>) : ListUiState
}

@HiltViewModel
class MemoryListViewModel @Inject constructor(
    private val repository: MemoryRepository,
) : ViewModel() {

    val uiState: StateFlow<ListUiState> = repository.allMemories
        .map { memories ->
            if (memories.isEmpty()) ListUiState.Empty
            else ListUiState.Loaded(memories)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState.Loading)

    fun delete(id: String) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun update(id: String, newItem: String, newLocation: String) {
        viewModelScope.launch {
            repository.updateMemory(id, newItem, newLocation)
        }
    }

    fun getLocationHistory(memoryId: String): Flow<List<LocationHistoryEntity>> =
        repository.getLocationHistory(memoryId)
}
