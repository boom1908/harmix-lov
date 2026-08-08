package com.boom.harmix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.harmix.extractor.StreamItem
import com.boom.harmix.metadata.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val items: List<StreamItem>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun loadRecommendations() {
        if (loaded) return
        loaded = true

        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val trending = metadataRepository.getTrending()
                _uiState.value = if (trending.isEmpty()) {
                    HomeUiState.Error("YouTube Music returned no trending tracks")
                } else {
                    HomeUiState.Success(trending)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error fetching trending tracks")
            }
        }
    }
}
