package com.stashed.app.ui.save

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.stashed.app.billing.BillingManager
import com.stashed.app.data.repository.MemoryRepository
import com.stashed.app.intelligence.EmojiMapper
import com.stashed.app.intelligence.NLParser
import com.stashed.app.intelligence.SpeechInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParsePreview(
    val emoji: String,
    val item: String,
    val location: String,
)

sealed interface SaveUiState {
    data object Idle : SaveUiState
    data object Saving : SaveUiState
    data object Success : SaveUiState
    data object LimitReached : SaveUiState
    data class Error(val message: String) : SaveUiState
}

@HiltViewModel
class SaveViewModel @Inject constructor(
    private val repository: MemoryRepository,
    private val billingManager: BillingManager,
    application: Application,
) : ViewModel() {

    companion object {
        const val FREE_TIER_LIMIT = 50
    }

    val inputText = MutableStateFlow("")
    val uiState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val speechInput = SpeechInput(application)
    val isPremium: StateFlow<Boolean> = billingManager.isPremium
    val selectedMediaPaths = MutableStateFlow<List<String>>(emptyList())

    fun addMediaPath(path: String) {
        if (selectedMediaPaths.value.size < 5) {
            selectedMediaPaths.value = selectedMediaPaths.value + path
        }
    }

    fun removeMediaPath(path: String) {
        selectedMediaPaths.value = selectedMediaPaths.value - path
    }

    init {
        // Pipe speech results into the text field
        viewModelScope.launch {
            speechInput.state.collect { state ->
                if (state is SpeechInput.State.Result) {
                    inputText.value = state.text
                }
            }
        }
    }

    /** Live parse preview — debounced so it doesn't run on every keystroke. */
    @OptIn(FlowPreview::class)
    val preview: StateFlow<ParsePreview?> = inputText
        .debounce(300)
        .map { text ->
            if (text.isBlank()) return@map null
            val parsed = NLParser.parse(text)
            if (parsed.item.isBlank()) return@map null
            ParsePreview(
                emoji = EmojiMapper.getEmoji(parsed.item),
                item = parsed.item,
                location = parsed.location,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onInputChange(text: String) {
        inputText.value = text
        if (uiState.value is SaveUiState.Error) uiState.value = SaveUiState.Idle
    }

    fun saveMemory() {
        val text = inputText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            // Check free tier limit
            if (!isPremium.value) {
                val count = repository.getCount()
                if (count >= FREE_TIER_LIMIT) {
                    uiState.value = SaveUiState.LimitReached
                    return@launch
                }
            }

            uiState.value = SaveUiState.Saving
            try {
                repository.saveMemory(text, selectedMediaPaths.value)
                uiState.value = SaveUiState.Success
                inputText.value = ""
                selectedMediaPaths.value = emptyList()
            } catch (e: Exception) {
                uiState.value = SaveUiState.Error(e.message ?: "Failed to save")
            }
        }
    }

    fun resetState() {
        uiState.value = SaveUiState.Idle
    }
}
