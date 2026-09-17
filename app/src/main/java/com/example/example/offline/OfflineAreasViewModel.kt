package com.example.example.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.example.domain.model.OfflineArea
import com.example.example.domain.repository.OfflineAreaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineAreasViewModel @Inject constructor(
    private val offlineAreaRepository: OfflineAreaRepository
) : ViewModel() {

    val areas: StateFlow<List<OfflineArea>> = offlineAreaRepository.observeOfflineAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onDeleteClicked(id: Long) {
        viewModelScope.launch {
            offlineAreaRepository.deleteOfflineArea(id)
        }
    }
}
