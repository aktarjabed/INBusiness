package com.aktarjabed.inbusiness.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class SplashState {
    object Loading : SplashState()
    object GoToDashboard : SplashState()
    object GoToSetup : SplashState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val businessContext: BusinessContext,
    private val businessRepository: BusinessRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SplashViewModel"
    }

    val splashState: StateFlow<SplashState> = businessContext.activeBusinessIdOrNull
        .map { businessId ->
            when {
                businessId.isNullOrBlank() -> {
                    // Never configured (or setup was interrupted): go to Setup.
                    SplashState.GoToSetup
                }
                else -> {
                    val businessExists = businessRepository.getBusinessDataById(businessId) != null
                    if (businessExists) {
                        SplashState.GoToDashboard
                    } else {
                        // The stored id has no matching row: the profile is gone (e.g. a
                        // partial restore). Setup is the only recovery path, and re-running
                        // it is safe because the old id can never resolve again.
                        Log.w(TAG, "Active business id has no matching profile; routing to setup")
                        SplashState.GoToSetup
                    }
                }
            }
        }
        .catch { throwable ->
            // Previously swallowed silently. Log it so a systemic read failure is visible
            // instead of looking like a fresh install.
            Log.e(TAG, "Could not resolve the active business; routing to setup", throwable)
            emit(SplashState.GoToSetup)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SplashState.Loading
        )
}
