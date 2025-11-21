package com.aktarjabed.inbusiness.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.repository.AuthRepository
import com.aktarjabed.inbusiness.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            authRepository.signInWithGoogle(activity)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                    Timber.i("Google sign-in successful")
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Google sign-in failed"
                    )
                    Timber.e(error, "Google sign-in failed")
                }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _uiState.value = AuthUiState.Error("Please enter email and password")
                return@launch
            }

            _uiState.value = AuthUiState.Loading

            authRepository.signInWithEmail(email, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                    Timber.i("Email sign-in successful")
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Email sign-in failed"
                    )
                    Timber.e(error, "Email sign-in failed")
                }
        }
    }

    fun sendOTP(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!phoneNumber.startsWith("+91") || phoneNumber.length != 13) {
            onError(Exception("Invalid phone number format. Use +91XXXXXXXXXX"))
            return
        }

        authRepository.sendOTP(
            activity = activity,
            phoneNumber = phoneNumber,
            onCodeSent = onCodeSent,
            onError = onError
        )
    }

    fun verifyOTP(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            authRepository.verifyOTP(verificationId, code)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                    onSuccess()
                    Timber.i("OTP verification successful")
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "OTP verification failed"
                    )
                    onError(error as Exception)
                    Timber.e(error, "OTP verification failed")
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.value = AuthUiState.Initial
                Timber.i("Sign-out successful")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Sign out failed")
                Timber.e(e, "Sign-out failed")
            }
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Initial
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}