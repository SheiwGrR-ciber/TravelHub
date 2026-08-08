package com.travelhub.ui.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.model.ItineraryResponse
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import com.travelhub.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItineraryUiState(
    val itineraries: List<ItineraryResponse> = emptyList(),
    val showCreateForm: Boolean = false,
    val newDay: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val deletingId: Int? = null,
    val errorMessage: String? = null
)

class ItineraryViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItineraryUiState())
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        val token = tokenOrError() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.itineraries(token)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(itineraries = result.value, isLoading = false)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun showCreateForm(show: Boolean) = _uiState.update {
        it.copy(showCreateForm = show, newDay = if (show) it.newDay else "", errorMessage = null)
    }

    fun updateDay(value: String) = _uiState.update {
        it.copy(newDay = value.filter(Char::isDigit).take(3), errorMessage = null)
    }

    fun create() {
        val day = _uiState.value.newDay.toIntOrNull()
        if (day == null || day <= 0) {
            _uiState.update { it.copy(errorMessage = "Ingresa un número de día válido") }
            return
        }
        val token = tokenOrError() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = repository.createItinerary(token, day)) {
                is RepositoryResult.Success -> _uiState.update { state ->
                    state.copy(
                        itineraries = (state.itineraries + result.value).sortedBy { it.day },
                        showCreateForm = false,
                        newDay = "",
                        isSubmitting = false
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = result.message)
                }
            }
        }
    }

    fun delete(id: Int) {
        val token = tokenOrError() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(deletingId = id, errorMessage = null) }
            when (val result = repository.deleteItinerary(token, id)) {
                is RepositoryResult.Success -> _uiState.update { state ->
                    state.copy(itineraries = state.itineraries.filterNot { it.id == id }, deletingId = null)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(deletingId = null, errorMessage = result.message)
                }
            }
        }
    }

    private fun tokenOrError(): String? = TokenManager.getToken().also { token ->
        if (token == null) _uiState.update { it.copy(isLoading = false, errorMessage = "La sesión ha expirado") }
    }
}
