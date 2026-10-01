package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.invoice.GstCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val businessContext: BusinessContext
) : ViewModel() {

    private val _setupComplete = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean> = _setupComplete

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setupBusiness(name: String, address: String, gstin: String?) {
        viewModelScope.launch {
            try {
                val trimmedName = name.trim()
                val trimmedAddress = address.trim()
                // Normalize before validating: GSTINs are case-insensitive and users often
                // paste them with padding. A stored "27abc...1z5" would otherwise fail GSTIN
                // validation later and silently block every invoice (supply type UNKNOWN).
                val normalizedGstin = gstin?.trim()?.uppercase().orEmpty()

                if (trimmedName.isBlank() || trimmedAddress.isBlank()) {
                    _error.value = "Name and Address are required"
                    return@launch
                }

                if (normalizedGstin.isNotEmpty() && !GstCalculator.isValidGstin(normalizedGstin)) {
                    _error.value = "That GSTIN does not look valid. Enter a 15-character GSTIN or leave it blank."
                    return@launch
                }

                val userId = UUID.randomUUID().toString()
                val businessId = UUID.randomUUID().toString()

                val businessData = BusinessData(
                    id = businessId,
                    name = trimmedName,
                    address = trimmedAddress,
                    gstin = normalizedGstin,
                    email = "",
                    phoneNumber = ""
                )

                val result = businessRepository.saveBusinessData(businessData)

                if (result.isSuccess) {
                    // Initialize context. If the profile is saved but the context write fails the
                    // app would keep pointing at a missing business, so surface it clearly.
                    try {
                        businessContext.setUserId(userId)
                        businessContext.setActiveBusinessId(businessId)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        _error.value = "Business was saved but could not be activated: ${e.message ?: "unknown error"}"
                        return@launch
                    }
                    _setupComplete.value = true
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception is kotlinx.coroutines.CancellationException) throw exception
                    _error.value = exception?.message ?: "Failed to set up business"
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _error.value = e.message ?: "Failed to set up business"
            }
        }
    }
}
