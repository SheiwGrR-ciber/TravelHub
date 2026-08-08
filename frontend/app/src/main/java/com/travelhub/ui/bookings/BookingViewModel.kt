package com.travelhub.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.model.BookingResponse
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import com.travelhub.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingUiState(
    val bookings: List<BookingResponse> = emptyList(),
    val isLoading: Boolean = true,
    val updatingBookingId: Int? = null,
    val errorMessage: String? = null
)

class BookingViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init { loadBookings() }

    fun loadBookings() {
        val token = TokenManager.getToken()
        if (token == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "La sesion ha expirado") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.bookings(token)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(bookings = result.value, isLoading = false)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun updateStatus(bookingId: Int, status: String) {
        val token = TokenManager.getToken() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(updatingBookingId = bookingId, errorMessage = null) }
            when (val result = repository.updateBookingStatus(token, bookingId, status)) {
                is RepositoryResult.Success -> _uiState.update { state ->
                    state.copy(
                        bookings = state.bookings.map { if (it.id == bookingId) result.value else it },
                        updatingBookingId = null
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(updatingBookingId = null, errorMessage = result.message)
                }
            }
        }
    }
}
