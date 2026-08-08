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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginCompleted: Boolean = false
)

class LoginViewModel(
    private val repository: TravelHubRepository = TravelHubRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) = _uiState.update { it.copy(email = value.trim(), errorMessage = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    fun login() {
        val snapshot = _uiState.value
        if (snapshot.email.isBlank() || snapshot.password.isBlank() || snapshot.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.login(snapshot.email, snapshot.password)) {
                is RepositoryResult.Success -> {
                    val (token, user) = result.value
                    TokenManager.saveToken(token)
                    TokenManager.saveUser(user.id, user.email, user.role)
                    _uiState.update { it.copy(isLoading = false, loginCompleted = true) }
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun consumeLoginCompleted() = _uiState.update { it.copy(loginCompleted = false) }
}
