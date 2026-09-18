package com.example.example.apikey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.example.domain.repository.ApiKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApiKeyUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ApiKeyViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeyUiState())
    val uiState: StateFlow<ApiKeyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (!apiKeyRepository.getApiKey().isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(saved = true)
            }
        }
    }

    fun onSaveClicked(key: String) {
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ключ не может быть пустым")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            apiKeyRepository.saveApiKey(key.trim())
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }
}
