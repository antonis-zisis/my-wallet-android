package com.mywallet.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mywallet.android.data.repository.AuthRepository
import com.mywallet.android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val fullName: String = "",
    val editedFullName: String = "",
    val isSavingName: Boolean = false,
    val nameSaved: Boolean = false,
    val nameError: String? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
    val passwordChanged: Boolean = false,
    val passwordError: String? = null,
    val isSigningOut: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userRepository.getMe().fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        email = user.email,
                        fullName = user.fullName ?: "",
                        editedFullName = user.fullName ?: "",
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        email = authRepository.currentUserEmail() ?: "",
                    )
                }
            )
        }
    }

    fun onFullNameChange(name: String) {
        _uiState.value = _uiState.value.copy(
            editedFullName = name,
            nameSaved = false,
            nameError = null,
        )
    }

    fun saveFullName() {
        val name = _uiState.value.editedFullName.trim()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingName = true, nameError = null)
            userRepository.updateMe(name).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isSavingName = false,
                        fullName = user.fullName ?: "",
                        nameSaved = true,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSavingName = false,
                        nameError = e.message ?: "Failed to save",
                    )
                }
            )
        }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            newPassword = password,
            passwordChanged = false,
            passwordError = null,
        )
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = password,
            passwordChanged = false,
            passwordError = null,
        )
    }

    fun changePassword() {
        val state = _uiState.value
        if (state.newPassword.length < 6) {
            _uiState.value = state.copy(passwordError = "Password must be at least 6 characters")
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.value = state.copy(passwordError = "Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangingPassword = true, passwordError = null)
            authRepository.updatePassword(state.newPassword).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        newPassword = "",
                        confirmPassword = "",
                        passwordChanged = true,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        passwordError = e.message ?: "Failed to change password",
                    )
                }
            )
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigningOut = true)
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(isSigningOut = false)
            onSignedOut()
        }
    }
}
