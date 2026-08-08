package com.travelhub.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.model.ServiceCreate
import com.travelhub.data.model.ServiceUpdate
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import com.travelhub.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManageServiceUiState(
    val serviceId: Int = 0,
    val type: String = "guia",
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val location: String = "",
    val available: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
) {
    val isEditMode: Boolean get() = serviceId > 0
}

class ManageServiceViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManageServiceUiState())
    val uiState: StateFlow<ManageServiceUiState> = _uiState.asStateFlow()
    private var initializedId: Int? = null

    fun initialize(serviceId: Int) {
        if (initializedId == serviceId) return
        initializedId = serviceId
        _uiState.update { it.copy(serviceId = serviceId, isLoading = serviceId > 0) }
        if (serviceId == 0) return
        viewModelScope.launch {
            when (val result = repository.service(serviceId)) {
                is RepositoryResult.Success -> _uiState.update {
                    val service = result.value
                    it.copy(
                        type = service.type,
                        name = service.name,
                        description = service.description.orEmpty(),
                        price = service.price.toString(),
                        location = service.location.orEmpty(),
                        available = service.available,
                        isLoading = false
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun updateType(value: String) = update { copy(type = value) }
    fun updateName(value: String) = update { copy(name = value) }
    fun updateDescription(value: String) = update { copy(description = value) }
    fun updateLocation(value: String) = update { copy(location = value) }
    fun updateAvailable(value: Boolean) = update { copy(available = value) }
    fun updatePrice(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) update { copy(price = value) }
    }

    fun save() {
        val state = _uiState.value
        val price = state.price.toDoubleOrNull()
        if (state.name.isBlank() || state.description.isBlank() || state.location.isBlank() || price == null || price <= 0) {
            _uiState.update { it.copy(errorMessage = "Completa todos los campos requeridos") }
            return
        }
        val token = TokenManager.getToken()
        if (token == null) {
            _uiState.update { it.copy(errorMessage = "La sesión ha expirado") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (state.isEditMode) {
                repository.updateService(token, state.serviceId, ServiceUpdate(
                    state.type, state.name.trim(), state.description.trim(), price,
                    state.location.trim(), state.available
                ))
            } else {
                repository.createService(token, ServiceCreate(
                    state.type, state.name.trim(), state.description.trim(), price, state.location.trim()
                ))
            }
            when (result) {
                is RepositoryResult.Success -> _uiState.update { it.copy(isSaving = false, saved = true) }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }

    fun consumeSaved() = _uiState.update { it.copy(saved = false) }
    private fun update(transform: ManageServiceUiState.() -> ManageServiceUiState) =
        _uiState.update { it.transform().copy(errorMessage = null) }
}
