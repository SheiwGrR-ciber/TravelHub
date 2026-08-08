package com.travelhub.ui.services

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

data class ServiceDetailUiState(
    val service: ServiceResponse? = null,
    val bookings: List<BookingResponse> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    fun confirmedBooking(serviceId: Int): BookingResponse? =
        bookings.firstOrNull { it.service_id == serviceId && it.status == "confirmada" }
}

class ServiceDetailViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()
    private var loadedServiceId: Int? = null

    fun load(serviceId: Int, force: Boolean = false) {
        if (!force && loadedServiceId == serviceId && _uiState.value.service != null) return
        loadedServiceId = serviceId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val serviceResult = repository.service(serviceId)) {
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = serviceResult.message)
                }
                is RepositoryResult.Success -> {
                    val token = TokenManager.getToken()
                    val bookingList = if (token != null) {
                        when (val bookingResult = repository.bookings(token)) {
                            is RepositoryResult.Success -> bookingResult.value
                            is RepositoryResult.Error -> emptyList()
                        }
                    } else emptyList()
                    _uiState.update {
                        it.copy(service = serviceResult.value, bookings = bookingList, isLoading = false)
                    }
                }
            }
        }
    }
}
