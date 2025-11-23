package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.domain.quotas.QuotaGate
import com.aktarjabed.inbusiness.domain.quotas.QuotaVerdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDateTime

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val quotaGate: QuotaGate
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Loading)
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    fun createInvoice(userId: String) {
        viewModelScope.launch {
            val verdict = quotaGate.assertQuota(userId)

            when (verdict) {
                is QuotaVerdict.Allowed -> {
                    _uiState.value = InvoiceUiState.CreateAllowed(
                        remainingToday = verdict.remaining
                    )
                }
                is QuotaVerdict.DailyCap -> {
                    _uiState.value = InvoiceUiState.Blocked(
                        reason = "Daily limit reached (${verdict.limit} invoices)",
                        resetTime = verdict.resetTime
                    )
                }
                is QuotaVerdict.MonthlyCap -> {
                    _uiState.value = InvoiceUiState.Blocked(
                        reason = "Monthly limit reached (60 invoices)"
                    )
                }
                is QuotaVerdict.FreeExpired -> {
                    _uiState.value = InvoiceUiState.Expired
                }
                is QuotaVerdict.Killed -> {
                    _uiState.value = InvoiceUiState.ServiceUnavailable
                }
            }
        }
    }
}

sealed class InvoiceUiState {
    object Loading : InvoiceUiState()
    data class CreateAllowed(val remainingToday: Int) : InvoiceUiState()
    data class Blocked(
        val reason: String,
        val resetTime: LocalDateTime? = null
    ) : InvoiceUiState()
    object Expired : InvoiceUiState()
    object ServiceUnavailable : InvoiceUiState()
}
