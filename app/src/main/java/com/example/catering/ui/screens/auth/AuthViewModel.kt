package com.example.catering.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catering.data.model.Role
import com.example.catering.data.model.User
import com.example.catering.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.currentUser.collectLatest { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Introduce un correo y una contraseña.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.login(email, password)
            result.onSuccess {
                _uiState.update { state -> state.copy(isLoading = false, errorMessage = null) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo iniciar sesión. Revisa tus datos e inténtalo de nuevo."
                    )
                }
            }
        }
    }

    fun register(data: RegistrationData) {
        if (!data.isValid()) {
            _uiState.update {
                it.copy(errorMessage = "Revisa que toda la información sea válida.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.register(
                email = data.email,
                password = data.password,
                role = data.role,
                displayName = data.displayName,
                affiliation = data.affiliation,
                address = data.address
            )
            result.onSuccess {
                _uiState.update { state -> state.copy(isLoading = false, errorMessage = null) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo crear la cuenta. Inténtalo de nuevo más tarde."
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: User? = null
)

data class RegistrationData(
    val email: String,
    val password: String,
    val role: Role,
    val displayName: String,
    val affiliation: String?,
    val address: String
) {
    fun isValid(): Boolean {
        val hasRequired = email.isNotBlank() && password.isNotBlank() &&
            displayName.isNotBlank() && address.isNotBlank()
        return hasRequired
    }
}
