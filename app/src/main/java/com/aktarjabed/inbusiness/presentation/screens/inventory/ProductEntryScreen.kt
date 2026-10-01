package com.aktarjabed.inbusiness.presentation.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.components.SearchableDropdownField
import com.aktarjabed.inbusiness.presentation.viewmodel.ProductViewModel
import com.aktarjabed.inbusiness.presentation.viewmodel.SaveProductState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEntryScreen(
    productId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val editingProduct by viewModel.editingProduct.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val existingCategories by viewModel.existingCategories.collectAsState()
    val existingUnitTypes by viewModel.existingUnitTypes.collectAsState()

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("") }
    var pricePerUnitStr by remember { mutableStateOf("") }
    var availableStockStr by remember { mutableStateOf("") }
    var reorderThresholdStr by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var isWholesaleOnly by remember { mutableStateOf(false) }
    var gstPercentageStr by remember { mutableStateOf("") }

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProductForEditing(productId)
        } else {
            viewModel.clearEditingProduct()
        }
    }

    LaunchedEffect(editingProduct) {
        editingProduct?.let { product ->
            name = product.name
            brand = product.brand
            category = product.category
            unitType = product.unitType
            pricePerUnitStr = product.pricePerUnit.toString()
            availableStockStr = product.availableStock.toString()
            reorderThresholdStr = product.reorderThreshold.toString()
            batchNumber = product.batchNumber
            isWholesaleOnly = product.isWholesaleOnly
            gstPercentageStr = if (product.gstPercentage > 0.0) product.gstPercentage.toString() else ""
        }
    }

    val parsedPrice = pricePerUnitStr.toDoubleOrNull()
    val priceError = when {
        pricePerUnitStr.isBlank() -> "Price is required."
        parsedPrice == null || !parsedPrice.isFinite() -> "Enter a finite price."
        parsedPrice < 0.0 -> "Price cannot be negative."
        else -> null
    }

    val parsedStock = availableStockStr.toDoubleOrNull()
    val stockError = when {
        availableStockStr.isBlank() -> "Stock is required."
        parsedStock == null || !parsedStock.isFinite() -> "Enter a finite stock quantity."
        parsedStock < 0.0 -> "Stock cannot be negative."
        else -> null
    }

    val parsedReorderThreshold = if (reorderThresholdStr.isBlank()) 0.0 else reorderThresholdStr.toDoubleOrNull()
    val reorderThresholdError = when {
        reorderThresholdStr.isBlank() -> null
        parsedReorderThreshold == null || !parsedReorderThreshold.isFinite() -> "Enter a finite reorder threshold."
        parsedReorderThreshold < 0.0 -> "Reorder threshold cannot be negative."
        else -> null
    }

    val parsedGstRate = if (gstPercentageStr.isBlank()) 0.0 else gstPercentageStr.toDoubleOrNull()
    val gstError = when {
        gstPercentageStr.isBlank() -> null
        parsedGstRate == null || !parsedGstRate.isFinite() -> "Enter a finite GST percentage."
        parsedGstRate < 0.0 -> "GST percentage cannot be negative."
        else -> null
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveProductState.Success -> {
                viewModel.resetSaveState()
                onNavigateBack()
            }
            is SaveProductState.Error -> {
                snackbarHostState.showSnackbar((saveState as SaveProductState.Error).message)
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Add Product" else "Edit Product") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            SearchableDropdownField(
                value = category,
                onValueChange = { category = it },
                label = "Category",
                suggestions = existingCategories
            )

            SearchableDropdownField(
                value = unitType,
                onValueChange = { unitType = it },
                label = "Unit Type (e.g., kg, L, bag)",
                suggestions = existingUnitTypes
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = pricePerUnitStr,
                    onValueChange = { pricePerUnitStr = it },
                    label = { Text("Price per Unit") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = priceError != null,
                    supportingText = { if (priceError != null) Text(priceError) }
                )

                OutlinedTextField(
                    value = availableStockStr,
                    onValueChange = { availableStockStr = it },
                    label = { Text(if (productId == null) "Opening Stock" else "Set Stock Quantity") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = stockError != null,
                    supportingText = { if (stockError != null) Text(stockError) }
                )
            }

            OutlinedTextField(
                value = reorderThresholdStr,
                onValueChange = { reorderThresholdStr = it },
                label = { Text("Low-stock alert threshold (optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = reorderThresholdError != null,
                supportingText = { if (reorderThresholdError != null) Text(reorderThresholdError) }
            )

            OutlinedTextField(
                value = batchNumber,
                onValueChange = { batchNumber = it },
                label = { Text("Batch Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = gstPercentageStr,
                onValueChange = { gstPercentageStr = it },
                label = { Text("GST Percentage (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = gstError != null,
                supportingText = { if (gstError != null) Text(gstError) }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isWholesaleOnly,
                    onCheckedChange = { isWholesaleOnly = it }
                )
                Text("Wholesale Only")
            }

            Button(
                onClick = {
                    val price = parsedPrice ?: return@Button
                    val stock = parsedStock ?: return@Button
                    if (gstError != null || reorderThresholdError != null) return@Button
                    viewModel.saveProduct(
                        id = productId ?: 0L,
                        name = name,
                        brand = brand,
                        category = category,
                        unitType = unitType,
                        pricePerUnit = price,
                        availableStock = stock,
                        batchNumber = batchNumber,
                        isWholesaleOnly = isWholesaleOnly,
                        gstPercentage = parsedGstRate ?: 0.0,
                        reorderThreshold = parsedReorderThreshold ?: 0.0
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SaveProductState.Loading &&
                    priceError == null && stockError == null &&
                    gstError == null && reorderThresholdError == null
            ) {
                if (saveState is SaveProductState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Product")
                }
            }
        }
    }
}
