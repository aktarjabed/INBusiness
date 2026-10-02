package com.aktarjabed.inbusiness.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.data.repository.ProductRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SaveProductState {
    object Idle : SaveProductState()
    object Loading : SaveProductState()
    object Success : SaveProductState()
    data class Error(val message: String) : SaveProductState()
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val businessContext: BusinessContext
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)

    /**
     * Products for the current search/category filter.
     *
     * The `debounce` is load-bearing: the search SQL is `LIKE '%query%'` (a leading wildcard
     * means no index can be used, so it is a full scan of the business's products), and the
     * query string is bound as a Room parameter on every emission. Without a debounce, typing
     * "fertilizer" runs ten full scans and re-emits ten times on the main-thread-adjacent UI
     * path. 300 ms matches InvoiceHistoryViewModel's search debounce.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = combine(searchQuery, selectedCategory) { query, category ->
        Pair(query, category)
    }.debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { (query, category) ->
            if (category == null) {
                productRepository.searchProducts(query)
            } else {
                productRepository.searchProductsByCategory(query, category)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val existingCategories: StateFlow<List<String>> = productRepository.getUniqueCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val existingUnitTypes: StateFlow<List<String>> = productRepository.getUniqueUnitTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveState = MutableStateFlow<SaveProductState>(SaveProductState.Idle)
    val saveState: StateFlow<SaveProductState> = _saveState.asStateFlow()

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct.asStateFlow()

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSelectedCategory(category: String?) {
        selectedCategory.value = category
    }

    fun resetSaveState() {
        _saveState.value = SaveProductState.Idle
    }

    fun loadProductForEditing(productId: Long) {
        viewModelScope.launch {
            try {
                val product = productRepository.getProductById(productId)
                _editingProduct.value = product
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Handle error softly
            }
        }
    }

    fun clearEditingProduct() {
        _editingProduct.value = null
    }

    fun saveProduct(
        id: Long = 0,
        name: String,
        brand: String,
        category: String,
        unitType: String,
        pricePerUnit: Double,
        availableStock: Double,
        batchNumber: String?,
        isWholesaleOnly: Boolean,
        gstPercentage: Double,
        reorderThreshold: Double = 0.0
    ) {
        viewModelScope.launch {
            _saveState.value = SaveProductState.Loading
            try {
                productRepository.saveProduct(
                    id = id,
                    name = name,
                    brand = brand,
                    category = category,
                    unitType = unitType,
                    pricePerUnit = pricePerUnit,
                    availableStock = availableStock,
                    batchNumber = batchNumber,
                    isWholesaleOnly = isWholesaleOnly,
                    gstPercentage = gstPercentage,
                    reorderThreshold = reorderThreshold
                )
                _saveState.value = SaveProductState.Success
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _saveState.value = SaveProductState.Error(e.message ?: "Failed to save product")
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(productId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Previously swallowed: a blocked delete (ledger history) looked like a no-op.
                Log.e(TAG, "Failed to delete product $productId: ${e.message}", e)
                _saveState.value = SaveProductState.Error(e.message ?: "Failed to delete product")
            }
        }
    }

    companion object {
        private const val TAG = "ProductViewModel"

        /** See [products]: search runs a full scan per keystroke without this. */
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
