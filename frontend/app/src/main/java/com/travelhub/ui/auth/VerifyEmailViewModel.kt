package com.travelhub.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelhub.data.repository.RepositoryResult
import com.travelhub.data.repository.TravelHubRepository
import com.travelhub.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyEmailUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val verificationCompleted: Boolean = false
)

class VerifyEmailViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(VerifyEmailUiState())
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    fun updateCode(value: String) {
        val digits = value.filter(Char::isDigit).take(6)
        _uiState.update { it.copy(code = digits, errorMessage = null, successMessage = null) }
    }

    fun verify(email: String) {
        val code = _uiState.value.code
        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "Ingresa el codigo de 6 digitos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.verifyEmail(email, code)) {
                is RepositoryResult.Success -> {
                    val (token, user) = result.value
                    TokenManager.saveToken(token)
                    TokenManager.saveUser(user.id, user.email, user.role)
                    _uiState.update { it.copy(isLoading = false, verificationCompleted = true) }
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun resend(email: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.resendVerificationCode(email)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isLoading = false, successMessage = "Codigo reenviado a tu correo")
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun consumeVerificationCompleted() =
        _uiState.update { it.copy(verificationCompleted = false) }
}
