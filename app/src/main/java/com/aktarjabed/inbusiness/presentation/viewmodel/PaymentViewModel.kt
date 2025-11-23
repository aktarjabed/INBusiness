package com.aktarjabed.inbusiness.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.local.entities.UserEntity
import com.aktarjabed.inbusiness.data.repository.AuthRepository
import com.aktarjabed.inbusiness.data.repository.PaymentRepository
import com.aktarjabed.inbusiness.domain.model.SubscriptionPlan
import com.aktarjabed.inbusiness.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val currentUser = authRepository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        checkSubscriptionStatus()
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.value = _uiState.value.copy(
            selectedPlan = plan,
            amount = plan.priceINR
        )
    }

    fun initiatePayment(activity: Activity, plan: SubscriptionPlan) {
        val user = currentUser.value?.toUserEntity()
        if (user == null) {
            _uiState.value = _uiState.value.copy(error = "User not authenticated")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        paymentRepository.initiatePayment(
            activity = activity,
            plan = plan,
            user = user,
            onResult = { result ->
                viewModelScope.launch {
                    result.onSuccess { paymentId ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showSuccessDialog = true,
                            selectedPlan = plan
                        )
                        Timber.i("Payment successful: $paymentId")
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                        Timber.e(error, "Payment failed")
                    }
                }
            }
        )
    }

    fun checkSubscriptionStatus() {
        viewModelScope.launch {
            val userId = currentUser.value?.id ?: return@launch

            paymentRepository.handleSubscriptionRenewal(userId)
                .onSuccess { plan ->
                    _uiState.value = _uiState.value.copy(currentPlan = plan)
                }
                .onFailure { error ->
                    Timber.e(error, "Subscription check failed")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(showSuccessDialog = false)
    }

    private fun User.toUserEntity(): UserEntity {
        return UserEntity(
            uid = id,
            email = email,
            phone = phoneNumber,
            displayName = displayName,
            photoUrl = photoUrl,
            provider = provider.name,
            isEmailVerified = isEmailVerified,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
    }
}

data class PaymentUiState(
    val selectedPlan: SubscriptionPlan? = null,
    val currentPlan: SubscriptionPlan? = null,
    val amount: Int = 0,
    val isLoading: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val error: String? = null
)