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

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val items: List<StreamItem>) : SearchUiState()
    data class Error(val message: String, val offline: Boolean = false) : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    fun runSearch() {
        val currentQuery = _query.value.trim()
        if (currentQuery.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val results = metadataRepository.search(currentQuery)
                _uiState.value = if (results.isEmpty()) {
                    SearchUiState.Error("No results found")
                } else {
                    SearchUiState.Success(results)
                }
            } catch (e: com.boom.harmix.core.OfflineException) {
                _uiState.value = SearchUiState.Error(e.message ?: "You're offline.", offline = true)
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Unknown search error")
            }
        }
    }
}
