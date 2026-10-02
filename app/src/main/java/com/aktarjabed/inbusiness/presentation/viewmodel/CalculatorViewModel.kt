package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.CalculationResult
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.models.FinancialMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Business calculator.
 *
 * Scenarios are stored in the same `business_data` table as the live business profile
 * (a schema limitation that is tracked as a follow-up). Because of that, the active
 * business row must never be offered as a deletable scenario: deleting it would
 * cascade into calculation results and is blocked only by the customer/payment
 * foreign keys.
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val repo: BusinessRepository,
    private val businessContext: BusinessContext
) : ViewModel() {

    private val _businessData = MutableStateFlow(BusinessData())
    val businessData: StateFlow<BusinessData> = _businessData.asStateFlow()

    private val _financialMetrics = MutableStateFlow(FinancialMetrics())
    val financialMetrics: StateFlow<FinancialMetrics> = _financialMetrics.asStateFlow()

    private val _savedScenarios = MutableStateFlow<List<BusinessData>>(emptyList())
    val savedScenarios: StateFlow<List<BusinessData>> = _savedScenarios.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // extraBufferCapacity keeps tryEmit() from blocking: a plain MutableSharedFlow() has
    // no buffer, so emit() suspends until a collector appears and the caller (e.g. the
    // delete coroutine) can hang forever when the screen is not subscribed.
    private val _errorMsg = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMsg: SharedFlow<String> = _errorMsg.asSharedFlow()

    init {
        loadSavedScenarios()
        calculateMetrics() // initial calc
    }

    fun updateBusinessData(data: BusinessData) {
        _businessData.value = data
        calculateMetrics()
    }

    private fun calculateMetrics() {
        _financialMetrics.value = repo.calculateFinancialMetrics(_businessData.value)
    }

    fun saveScenario(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = _businessData.value.copy(
                    id = UUID.randomUUID().toString(),
                    scenarioName = name.ifBlank { "Scenario ${System.currentTimeMillis()}" }
                )
                // BusinessRepository reports failures as Result.failure instead of throwing,
                // so they must be inspected: otherwise a failed save closed the dialog and
                // looked successful while nothing was written.
                val saveResult = repo.saveBusinessData(data)
                if (saveResult.isFailure) {
                    _errorMsg.tryEmit("Failed to save: ${describe(saveResult.exceptionOrNull())}")
                    return@launch
                }

                val metrics = _financialMetrics.value
                val metricsResult = repo.saveCalculationResult(
                    CalculationResult(
                        id = UUID.randomUUID().toString(),
                        businessDataId = data.id,
                        grossProfit = metrics.grossProfit,
                        ebitda = metrics.ebitda,
                        netProfit = metrics.netProfit,
                        gstPayable = metrics.gstPayable,
                        breakEvenPoint = metrics.breakEvenPoint,
                        cashFlow = metrics.cashFlow,
                        grossMargin = metrics.grossMargin,
                        netMargin = metrics.netMargin,
                        operatingMargin = metrics.operatingMargin,
                        roi = metrics.roi
                    )
                )
                if (metricsResult.isFailure) {
                    _errorMsg.tryEmit(
                        "Scenario saved, but its metrics could not be stored: " +
                            describe(metricsResult.exceptionOrNull())
                    )
                }
                // No manual reload: `savedScenarios` is collected from the Room flow in
                // [loadSavedScenarios], which re-emits on every write. Calling it again here
                // used to leak another permanent collector per save.
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _errorMsg.tryEmit("Failed to save: ${describe(e)}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadScenario(data: BusinessData) {
        _businessData.value = data
        calculateMetrics()
    }

    fun deleteScenario(data: BusinessData) {
        viewModelScope.launch {
            try {
                val activeBusinessId = businessContext.activeBusinessId.first()
                if (data.id == activeBusinessId) {
                    // Never allow the live business profile to be deleted from the calculator.
                    _errorMsg.tryEmit("The active business cannot be deleted from the calculator")
                    return@launch
                }
                val deleteResult = repo.deleteBusinessData(data)
                if (deleteResult.isFailure) {
                    // e.g. a RESTRICT foreign key still references this row. Without this check
                    // the failure was invisible and the scenario silently stayed in the list.
                    _errorMsg.tryEmit("Delete failed: ${describe(deleteResult.exceptionOrNull())}")
                    return@launch
                }
                repo.deleteAllCalculationResults(data.id)
                // The live Room flow re-emits; see the note in [saveScenario].
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _errorMsg.tryEmit("Delete failed: ${describe(e)}")
            }
        }
    }

    private fun describe(throwable: Throwable?): String =
        throwable?.localizedMessage?.takeIf { it.isNotBlank() } ?: throwable?.javaClass?.simpleName ?: "unknown error"

    private fun loadSavedScenarios() {
        viewModelScope.launch {
            try {
                val activeBusinessId = businessContext.activeBusinessId.first()
                repo.getAllBusinessData().collect { scenarios ->
                    // Hide the live business profile: this screen manages calculator
                    // scenarios only, and offering the business here invites accidents.
                    _savedScenarios.value = scenarios.filterNot { it.id == activeBusinessId }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _savedScenarios.value = emptyList()
            }
        }
    }
}
