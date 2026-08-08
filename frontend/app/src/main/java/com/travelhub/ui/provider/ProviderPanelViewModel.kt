package com.travelhub.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.model.BookingResponse
import com.travelhub.data.model.ServiceResponse
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import com.travelhub.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderPanelUiState(
    val services: List<ServiceResponse> = emptyList(),
    val bookings: List<BookingResponse> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ProviderPanelViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderPanelUiState())
    val uiState: StateFlow<ProviderPanelUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        val token = TokenManager.getToken()
        val userId = TokenManager.getUserId()
        if (token == null || userId == 0) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "La sesión ha expirado") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val services = when (val result = repository.services(null)) {
                is RepositoryResult.Success -> result.value.filter { it.provider_id == userId }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    return@launch
                }
            }
            val bookings = when (val result = repository.bookings(token)) {
                is RepositoryResult.Success -> result.value
                is RepositoryResult.Error -> emptyList()
            }
            _uiState.update { it.copy(services = services, bookings = bookings, isLoading = false) }
        }
    }

    fun delete(serviceId: Int) {
        val token = TokenManager.getToken() ?: return
        viewModelScope.launch {
            when (val result = repository.deleteService(token, serviceId)) {
                is RepositoryResult.Success -> _uiState.update { state ->
                    state.copy(services = state.services.filterNot { it.id == serviceId })
                }
                is RepositoryResult.Error -> _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }
}
