package com.travelhub.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.model.ServiceResponse
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val services: List<ServiceResponse> = emptyList(),
    val category: String = "todos", val location: String = "",
    val minPrice: String = "", val maxPrice: String = "",
    val minRating: Int = 0, val availability: Boolean? = true,
    val isLoading: Boolean = true, val errorMessage: String? = null
) {
    val advancedFilterCount: Int get() = listOf(
        location.isNotBlank(), minPrice.isNotBlank(), maxPrice.isNotBlank(),
        minRating > 0, availability != true
    ).count { it }
}

class CatalogViewModel(private val repository: TravelHubRepository = TravelHubRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init { loadServices() }

    fun selectCategory(value: String) {
        if (_uiState.value.category == value) return
        _uiState.update { it.copy(category = value) }
        loadServices()
    }
    fun setLocation(v: String) = _uiState.update { it.copy(location = v.take(150), errorMessage = null) }
    fun setMinPrice(v: String) = setPrice(v) { copy(minPrice = it) }
    fun setMaxPrice(v: String) = setPrice(v) { copy(maxPrice = it) }
    private fun setPrice(value: String, update: CatalogUiState.(String) -> CatalogUiState) {
        if (value.isEmpty() || value.matches(Regex("^\\d{0,7}([.]\\d{0,2})?$")))
            _uiState.update { state -> state.update(value).copy(errorMessage = null) }
    }
    fun setRating(value: Int) = _uiState.update { it.copy(minRating = value.coerceIn(0, 5)) }
    fun setAvailability(value: Boolean?) = _uiState.update { it.copy(availability = value) }

    fun clearFilters() {
        _uiState.update { it.copy(category = "todos", location = "", minPrice = "", maxPrice = "", minRating = 0, availability = true) }
        loadServices()
    }

    fun applyFilters(): Boolean {
        val min = _uiState.value.minPrice.toDoubleOrNull()
        val max = _uiState.value.maxPrice.toDoubleOrNull()
        if (min != null && max != null && min > max) {
            _uiState.update { it.copy(errorMessage = "El precio minimo no puede superar al maximo") }
            return false
        }
        loadServices()
        return true
    }

    fun loadServices() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.services(
                type = state.category.takeUnless { it == "todos" },
                location = state.location.trim().ifBlank { null },
                minPrice = state.minPrice.toDoubleOrNull(), maxPrice = state.maxPrice.toDoubleOrNull(),
                minRating = state.minRating.takeIf { it > 0 }?.toDouble(), available = state.availability
            )) {
                is RepositoryResult.Success -> _uiState.update { it.copy(services = result.value, isLoading = false) }
                is RepositoryResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
