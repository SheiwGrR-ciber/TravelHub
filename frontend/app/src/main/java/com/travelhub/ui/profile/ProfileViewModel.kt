package com.travelhub.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.model.UserProfileUpdate
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import com.travelhub.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true, val isSaving: Boolean = false,
    val name: String = "", val email: String = "", val role: String = "turista",
    val phone: String = "", val location: String = "", val bio: String = "",
    val businessName: String = "", val providerType: String = "guia",
    val experienceYears: String = "", val approved: Boolean = false,
    val error: String? = null, val success: String? = null
)

class ProfileViewModel(private val repository: TravelHubRepository = TravelHubRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        val token = TokenManager.getToken()
        if (token == null) {
            _uiState.update { it.copy(isLoading = false, error = "Sesion no disponible") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        when (val result = repository.profile(token)) {
            is RepositoryResult.Success -> result.value.let { profile ->
                TokenManager.saveUser(profile.id, profile.email, profile.role)
                _uiState.value = ProfileUiState(
                    isLoading = false, name = profile.name, email = profile.email, role = profile.role,
                    phone = profile.phone.orEmpty(), location = profile.location.orEmpty(), bio = profile.bio.orEmpty(),
                    businessName = profile.business_name.orEmpty(), providerType = profile.provider_type ?: "guia",
                    experienceYears = profile.experience_years?.toString().orEmpty(), approved = profile.approved
                )
            }
            is RepositoryResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
        }
    }

    fun setName(v: String) = edit { copy(name = v) }
    fun setPhone(v: String) = edit { copy(phone = v.filter { c -> c.isDigit() || c in "+ -()" }.take(30)) }
    fun setLocation(v: String) = edit { copy(location = v.take(150)) }
    fun setBio(v: String) = edit { copy(bio = v.take(500)) }
    fun setBusinessName(v: String) = edit { copy(businessName = v.take(150)) }
    fun setProviderType(v: String) = edit { copy(providerType = v) }
    fun setExperience(v: String) = edit { copy(experienceYears = v.filter(Char::isDigit).take(2)) }
    private fun edit(block: ProfileUiState.() -> ProfileUiState) = _uiState.update { it.block().copy(error = null, success = null) }

    fun save() = viewModelScope.launch {
        val state = _uiState.value
        if (state.name.trim().length < 2) {
            _uiState.update { it.copy(error = "Ingresa un nombre valido") }; return@launch
        }
        val years = state.experienceYears.toIntOrNull()
        if (state.role == "prestador" && state.experienceYears.isNotBlank() && (years == null || years > 80)) {
            _uiState.update { it.copy(error = "Los anos de experiencia no son validos") }; return@launch
        }
        val token = TokenManager.getToken() ?: return@launch
        _uiState.update { it.copy(isSaving = true, error = null, success = null) }
        val request = UserProfileUpdate(
            state.name.trim(), state.phone.trim().ifBlank { null }, state.location.trim().ifBlank { null },
            state.bio.trim().ifBlank { null }, state.businessName.trim().ifBlank { null },
            if (state.role == "prestador") state.providerType else null, years
        )
        when (val result = repository.updateProfile(token, request)) {
            is RepositoryResult.Success -> _uiState.update { it.copy(isSaving = false, success = "Perfil actualizado correctamente") }
            is RepositoryResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
        }
    }
}
