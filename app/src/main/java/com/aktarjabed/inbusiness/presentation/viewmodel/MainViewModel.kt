package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.repository.AuthRepository
import com.aktarjabed.inbusiness.data.remote.config.RemoteConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val remoteConfig: RemoteConfigRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isMaintenanceMode = MutableStateFlow(false)
    val isMaintenanceMode: StateFlow<Boolean> = _isMaintenanceMode.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        observeAuthState()
        fetchRemoteConfig()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUserFlow
                .onEach { user ->
                    _isAuthenticated.value = user != null
                    _isLoading.value = false
                }
                .catch { e ->
                    Timber.e(e, "Error observing auth state")
                    _isLoading.value = false
                }
                .collect()
        }
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            try {
                remoteConfig.fetch()
                _isMaintenanceMode.value = remoteConfig.isMaintenanceMode()
            } catch (e: Exception) {
                Timber.e(e, "Error fetching remote config")
            }
        }
    }

    fun onPaymentSuccess() {
        viewModelScope.launch {
            _events.emit(Event.ShowPaymentSuccess)
        }
    }

    fun onPaymentError(error: String) {
        viewModelScope.launch {
            _events.emit(Event.ShowError(Exception(error)))
        }
    }

    sealed class Event {
        object ShowPaymentSuccess : Event()
        data class ShowError(val throwable: Throwable) : Event()
    }
}