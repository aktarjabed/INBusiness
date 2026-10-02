package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.data.repository.ProductRepository
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.usecase.CreateInvoiceUseCase
import com.aktarjabed.inbusiness.domain.usecase.GetProductSuggestionsUseCase
import com.aktarjabed.inbusiness.domain.usecase.ProductSuggestion
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.invoice.CalculateInvoiceTotalsUseCase
import com.aktarjabed.inbusiness.domain.usecase.DetermineSupplyTypeUseCase
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCalculationResult
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val quotaGate: QuotaGate,
    private val createInvoiceUseCase: CreateInvoiceUseCase,
    private val calculateInvoiceTotalsUseCase: CalculateInvoiceTotalsUseCase,
    private val determineSupplyTypeUseCase: DetermineSupplyTypeUseCase,
    private val businessContext: BusinessContext,
    private val productRepository: ProductRepository,
    private val businessRepository: BusinessRepository,
    private val getProductSuggestionsUseCase: GetProductSuggestionsUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Initial)
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    // UI state for inputs.
    //
    // The draft is restored from [savedStateHandle] so process death (low memory, "don't keep
    // activities", swipe-away) does not discard a half-composed invoice; [persistDraft] mirrors
    // every change back. Seller fields are excluded on purpose: they come from the business
    // profile and are re-read on load.
    val customerName = MutableStateFlow(savedStateHandle.get<String>(KEY_CUSTOMER_NAME) ?: "")
    val customerGSTIN = MutableStateFlow(savedStateHandle.get<String>(KEY_CUSTOMER_GSTIN) ?: "")
    val buyerAddress = MutableStateFlow(savedStateHandle.get<String>(KEY_BUYER_ADDRESS) ?: "")

    val sellerName = MutableStateFlow("")
    val sellerAddress = MutableStateFlow("")
    val sellerGstin = MutableStateFlow("")
    val supplyType = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SUPPLY_TYPE)
            ?.let { stored -> runCatching { SupplyType.valueOf(stored) }.getOrNull() }
            ?: SupplyType.UNKNOWN
    )

    // Items state
    private val _invoiceItems = MutableStateFlow(
        InvoiceDraftCodec.decode(savedStateHandle.get<String>(KEY_DRAFT_ITEMS))
    )
    // Transient: an open edit dialog is not worth restoring, the items themselves are.
    private var editingItemIndex: Int? = null
    val invoiceItems = _invoiceItems.asStateFlow()

    private val _calculationResult = MutableStateFlow<InvoiceCalculationResult?>(null)
    val calculationResult: StateFlow<InvoiceCalculationResult?> = _calculationResult.asStateFlow()

    // Products for dropdown
    private val _productSuggestions = MutableStateFlow<List<ProductSuggestion>>(emptyList())
    val productSuggestions = _productSuggestions.asStateFlow()

    val amountPaid = MutableStateFlow(savedStateHandle.get<Double>(KEY_AMOUNT_PAID) ?: 0.0)
    val paymentMethod = MutableStateFlow(savedStateHandle.get<String>(KEY_PAYMENT_METHOD) ?: "NONE")

    // Error message for surfacing calculation errors to the UI
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Idempotency key for the *current* submission attempt.
     *
     * It is intentionally reused while the payload is unchanged so that a retry of
     * the exact same invoice cannot create a duplicate. As soon as any input that
     * feeds the request fingerprint changes, the key is discarded: reusing a key
     * with a different payload is a hard validation error
     * ("Idempotency key reused for a different payload") which would otherwise
     * permanently block the user from saving an edited invoice.
     */
    /**
     * Restored as well: a retry of the same payload after process death must reuse the key,
     * otherwise the retry would look like a brand-new invoice. It is cleared as soon as the
     * payload changes (see [invalidatePendingIdempotencyKey]).
     */
    private var currentIdempotencyKey: String? = savedStateHandle.get<String>(KEY_IDEMPOTENCY_KEY)

    /** Guards against double-tap submissions before the UI state flips to Loading. */
    private var invoiceCreationInFlight = false

    init {
        // Load product suggestions for autocomplete
        viewModelScope.launch {
            getProductSuggestionsUseCase().collect { suggestionsList ->
                _productSuggestions.value = suggestionsList
            }
        }

        // Observe business data for Seller GSTIN
        viewModelScope.launch {
            try {
                val currentBusinessId = businessContext.activeBusinessId.first()
                val businessData = businessRepository.getBusinessDataById(currentBusinessId)
                if (businessData != null) {
                    sellerName.value = businessData.name
                    sellerAddress.value = businessData.address
                    if (!businessData.gstin.isNullOrBlank()) {
                        sellerGstin.value = businessData.gstin
                    }

                    // Re-evaluate supply type if customer GSTIN is already provided
                    if (customerGSTIN.value.isNotBlank() && sellerGstin.value.isNotBlank()) {
                        supplyType.value = determineSupplyTypeUseCase(sellerGstin.value, customerGSTIN.value)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Failed to load business data for GSTIN", e)
            }
        }

        // Restored draft values only populate the inputs; totals are derived, so recompute them
        // or the screen returns from process death showing an empty summary.
        recalculateItems()
    }

    fun checkQuotaAndPrepare() {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading

            try {
                val currentUserId = businessContext.currentUserId.first()

                // Peek without consuming
                val verdict = quotaGate.assertQuota(currentUserId, consume = false)

                when (verdict) {
                    is QuotaVerdict.Allowed -> {
                        val nextInvoiceNumber = "Invoice number will be assigned when saved"
                        _uiState.value = InvoiceUiState.CreateAllowed(
                            remainingToday = verdict.remaining,
                            invoiceNumber = nextInvoiceNumber
                        )
                    }
                    is QuotaVerdict.DailyCap -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                    is QuotaVerdict.MonthlyCap -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                    is QuotaVerdict.FreeExpired -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error checking quota", e)
                _uiState.value = InvoiceUiState.Error("Failed to check quota: ${e.message}")
            }
        }
    }

    fun updateCustomerData(name: String, gstin: String, address: String) {
        // GSTINs are case-insensitive; normalize so supply-type detection (which is
        // case-sensitive) does not fail for pasted lowercase values.
        val normalizedGstin = gstin.trim().uppercase()

        customerName.value = name
        customerGSTIN.value = normalizedGstin
        buyerAddress.value = address
        invalidatePendingIdempotencyKey()

        // Auto-detect supply type
        if (sellerGstin.value.isNotBlank() && normalizedGstin.isNotBlank()) {
            supplyType.value = determineSupplyTypeUseCase(sellerGstin.value, normalizedGstin)
        }
        recalculateItems()
        persistDraft()
    }


    fun setAmountPaid(amount: Double) {
        val normalized = if (amount.isFinite()) java.math.BigDecimal(amount.toString()).setScale(2, java.math.RoundingMode.HALF_UP).toDouble() else 0.0
        amountPaid.value = normalized
        invalidatePendingIdempotencyKey()
        recalculateItems()
        persistDraft()
    }

    fun setPaymentMethod(method: String) {
        if (paymentMethod.value != method) {
            invalidatePendingIdempotencyKey()
        }
        paymentMethod.value = method
        persistDraft()
    }

    fun setSupplyType(type: SupplyType) {
        supplyType.value = type
        invalidatePendingIdempotencyKey()
        recalculateItems()
        persistDraft()
    }


    fun setEditingItemIndex(index: Int?) {
        editingItemIndex = index
    }

    fun addItem(item: InvoiceItemInput) {
        val currentItems = _invoiceItems.value.toMutableList()
        val index = editingItemIndex
        if (index != null && index in currentItems.indices) {
            currentItems[index] = item
        } else {
            currentItems.add(item)
        }
        editingItemIndex = null
        _invoiceItems.value = currentItems
        invalidatePendingIdempotencyKey()
        recalculateItems()
        persistDraft()
    }

    fun removeItem(index: Int) {
        val currentItems = _invoiceItems.value.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _invoiceItems.value = currentItems
            invalidatePendingIdempotencyKey()
            recalculateItems()
            persistDraft()
        }
    }

    /**
     * Drops the pending idempotency key. Called whenever the request payload changes so a
     * follow-up submission is treated as a new request instead of a conflicting replay.
     */
    private fun invalidatePendingIdempotencyKey() {
        currentIdempotencyKey = null
    }

    /**
     * Mirrors the in-progress invoice into [savedStateHandle] so it survives process death.
     *
     * Every mutator must call this: a new draft field that is not persisted here silently
     * disappears on restore. Seller fields and `editingItemIndex` are intentionally excluded.
     */
    private fun persistDraft() {
        savedStateHandle[KEY_CUSTOMER_NAME] = customerName.value
        savedStateHandle[KEY_CUSTOMER_GSTIN] = customerGSTIN.value
        savedStateHandle[KEY_BUYER_ADDRESS] = buyerAddress.value
        savedStateHandle[KEY_SUPPLY_TYPE] = supplyType.value.name
        savedStateHandle[KEY_DRAFT_ITEMS] = InvoiceDraftCodec.encode(_invoiceItems.value)
        savedStateHandle[KEY_AMOUNT_PAID] = amountPaid.value
        savedStateHandle[KEY_PAYMENT_METHOD] = paymentMethod.value
        savedStateHandle[KEY_IDEMPOTENCY_KEY] = currentIdempotencyKey
    }

    private fun recalculateItems() {
        val currentSupplyType = supplyType.value
        if (currentSupplyType == SupplyType.UNKNOWN) {
            _calculationResult.value = null
            return
        }

        try {
            val validInputs = _invoiceItems.value.filter { input ->
                val isBlank = input.description.isBlank() && input.quantity == 0.0 && input.pricePerUnit == 0.0 && input.gstPercentage == 0.0
                if (isBlank) return@filter false

                if (input.description.isBlank() || input.quantity <= 0 || input.pricePerUnit < 0 || input.gstPercentage < 0) {
                    throw IllegalArgumentException("Partially filled or invalid item found: ${input.description}")
                }
                true
            }

            val rawItems = validInputs.map { input ->
                InvoiceItem(
                    description = input.description,
                    quantity = input.quantity,
                    pricePerUnit = input.pricePerUnit,
                    unitType = input.unitType,
                    gstPercentage = input.gstPercentage,
                    productId = input.productId
                )
            }
            if (rawItems.isNotEmpty()) {
                val calcResult = calculateInvoiceTotalsUseCase(
                    items = rawItems,
                    supplyType = currentSupplyType,
                    amountPaid = amountPaid.value
                )
                _calculationResult.value = calcResult
                _errorMessage.value = null
            } else {
                _calculationResult.value = null
                _errorMessage.value = null
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Calculation error", e)
            _calculationResult.value = null
            _errorMessage.value = e.message ?: "Calculation failed"
        }
    }

    fun createInvoice() {
        if (invoiceCreationInFlight) return
        invoiceCreationInFlight = true

        viewModelScope.launch {
            try {
                val validInputs = _invoiceItems.value.filter { input ->
                    val isBlank = input.description.isBlank() && input.quantity == 0.0 && input.pricePerUnit == 0.0 && input.gstPercentage == 0.0
                    if (!isBlank && (input.description.isBlank() || input.quantity <= 0 || input.pricePerUnit < 0 || input.gstPercentage < 0)) {
                        _uiState.value = InvoiceUiState.Error("Partially filled or invalid item found")
                        return@launch
                    }
                    !isBlank
                }
                if (validInputs.isEmpty()) {
                    _uiState.value = InvoiceUiState.Error("Please add at least one item")
                    return@launch
                }

                if (customerName.value.isBlank()) {
                    _uiState.value = InvoiceUiState.Error("Please enter the customer name")
                    return@launch
                }

                if (customerGSTIN.value.isNotBlank() &&
                    !com.aktarjabed.inbusiness.domain.invoice.GstCalculator.isValidGstin(customerGSTIN.value)
                ) {
                    _uiState.value = InvoiceUiState.Error(
                        "Customer GSTIN '${customerGSTIN.value}' is not a valid GSTIN. Correct it or clear the field for an unregistered buyer."
                    )
                    return@launch
                }

                if (supplyType.value == SupplyType.UNKNOWN) {
                    _uiState.value = InvoiceUiState.Error(
                        "Supply type could not be determined. Select Intra-state or Inter-state, or add valid GSTINs."
                    )
                    return@launch
                }

                _uiState.value = InvoiceUiState.Loading

                val idempotencyKey = currentIdempotencyKey
                    ?: java.util.UUID.randomUUID().toString().also { currentIdempotencyKey = it }
                // Persist the key before submitting: a crash mid-submit must not let the retry
                // be treated as a new request.
                persistDraft()

                val rawItems = validInputs.map { input ->
                    InvoiceItem(
                        description = input.description,
                        quantity = input.quantity,
                        pricePerUnit = input.pricePerUnit,
                        unitType = input.unitType,
                        gstPercentage = input.gstPercentage,
                        productId = input.productId
                    )
                }

                val result = createInvoiceUseCase(
                    customerName = customerName.value,
                    customerGSTIN = customerGSTIN.value,
                    buyerAddress = buyerAddress.value,
                    supplyType = supplyType.value,
                    items = rawItems,
                    idempotencyKey = idempotencyKey,
                    amountPaid = amountPaid.value,
                    paymentMethod = paymentMethod.value
                )

                when(result) {
                    is InvoiceCreationResult.Success -> {
                        // The request is persisted and confirmed; a subsequent submission is a
                        // genuinely new invoice and must not reuse this key.
                        invalidatePendingIdempotencyKey()
                        persistDraft()
                        _uiState.value = InvoiceUiState.Success(
                            invoiceId = result.invoiceId,
                            message = "Invoice ${result.invoiceNumber} created successfully"
                        )
                    }
                    is InvoiceCreationResult.IdempotentReplay -> {
                        invalidatePendingIdempotencyKey()
                        persistDraft()
                         _uiState.value = InvoiceUiState.Success(
                            invoiceId = result.invoiceId,
                            message = "Invoice already created"
                        )
                    }
                    is InvoiceCreationResult.QuotaExceeded -> {
                        // "Quota Exceeded" alone gives the user nothing to act on; state which
                        // limit was hit and that the day/month rolls over automatically.
                        _uiState.value = InvoiceUiState.Error(
                            "Invoice limit reached for this period. Your free daily/monthly " +
                                "allowance resets automatically - try again after the reset, " +
                                "or upgrade for unlimited invoices."
                        )
                    }
                    is InvoiceCreationResult.InsufficientStock -> {
                         _uiState.value = InvoiceUiState.Error("Insufficient stock for ${result.productName}. Requested: ${result.requested}, Available: ${result.available}")
                    }
                    is InvoiceCreationResult.ProductNotFound -> {
                         _uiState.value = InvoiceUiState.Error("Product not found")
                    }
                    is InvoiceCreationResult.InvalidRequest -> {
                        _uiState.value = InvoiceUiState.Error(result.message)
                    }
                    is InvoiceCreationResult.UnexpectedFailure -> {
                        _uiState.value = InvoiceUiState.Error("Failed to create invoice: ${result.cause.message}")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error creating invoice", e)
                _uiState.value = InvoiceUiState.Error("Failed to create invoice: ${e.message}")
            } finally {
                invoiceCreationInFlight = false
            }
        }
    }

    fun resetState() {
        _uiState.value = InvoiceUiState.Initial
        invalidatePendingIdempotencyKey()
        persistDraft()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetIdempotencyKey() {
        currentIdempotencyKey = null
    }

    companion object {
        private const val TAG = "InvoiceViewModel"

        private const val KEY_CUSTOMER_NAME = "draft_customer_name"
        private const val KEY_CUSTOMER_GSTIN = "draft_customer_gstin"
        private const val KEY_BUYER_ADDRESS = "draft_buyer_address"
        private const val KEY_SUPPLY_TYPE = "draft_supply_type"
        private const val KEY_DRAFT_ITEMS = "draft_items"
        private const val KEY_AMOUNT_PAID = "draft_amount_paid"
        private const val KEY_PAYMENT_METHOD = "draft_payment_method"
        private const val KEY_IDEMPOTENCY_KEY = "draft_idempotency_key"
    }
}

// Temporary data class for UI input before mapping to domain InvoiceItem
data class InvoiceItemInput(
    val description: String,
    val quantity: Double,
    val pricePerUnit: Double,
    val gstPercentage: Double,
    val unitType: String = "",
    val productId: Long? = null // null for ad-hoc
) {
    val isAdHoc: Boolean get() = productId == null
}
