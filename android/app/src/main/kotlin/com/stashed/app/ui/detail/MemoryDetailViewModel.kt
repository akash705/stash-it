package com.stashed.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stashed.app.data.local.MemoryEntity
import com.stashed.app.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MemoryRepository,
) : ViewModel() {

    private val memoryId: String = checkNotNull(savedStateHandle["memoryId"])

    private val _memory = MutableStateFlow<MemoryEntity?>(null)
    val memory: StateFlow<MemoryEntity?> = _memory

    init {
        viewModelScope.launch {
            _memory.value = repository.getById(memoryId)
        }
    }
}
