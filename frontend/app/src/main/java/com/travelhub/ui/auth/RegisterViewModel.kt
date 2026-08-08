package com.travelhub.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val selectedRole: String = "turista",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationCompleted: Boolean = false
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
}

class RegisterViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateName(value: String) = update { copy(name = value) }
    fun updateEmail(value: String) = update { copy(email = value.trim()) }
    fun updatePassword(value: String) = update { copy(password = value) }
    fun updateConfirmPassword(value: String) = update { copy(confirmPassword = value) }
    fun selectRole(value: String) {
        if (value in setOf("turista", "prestador")) update { copy(selectedRole = value) }
    }

    fun register() {
        val snapshot = _uiState.value
        val validationError = validate(snapshot)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.register(
                snapshot.name.trim(),
                snapshot.email,
                snapshot.password,
                snapshot.selectedRole
            )) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isLoading = false, registrationCompleted = true)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun consumeRegistrationCompleted() =
        _uiState.update { it.copy(registrationCompleted = false) }

    private fun validate(state: RegisterUiState): String? = when {
        state.name.trim().length < 2 -> "Ingresa tu nombre completo"
        !Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> "Ingresa un correo valido"
        state.password.length < 8 -> "La contrasena debe tener al menos 8 caracteres"
        state.password.toByteArray().size > 72 -> "La contrasena es demasiado larga"
        state.password != state.confirmPassword -> "Las contrasenas no coinciden"
        else -> null
    }

    private fun update(transform: RegisterUiState.() -> RegisterUiState) =
        _uiState.update { it.transform().copy(errorMessage = null) }
}
