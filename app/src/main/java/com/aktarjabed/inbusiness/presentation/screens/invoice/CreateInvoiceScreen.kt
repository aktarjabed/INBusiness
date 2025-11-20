package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    invoiceId: String,
    onBack: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: CreateInvoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(invoiceId) {
        if (invoiceId.isNotBlank()) viewModel.loadInvoice(invoiceId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (invoiceId.isBlank()) "New Invoice" else "Edit Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveInvoice()
                            onSaveComplete()
                        },
                        enabled = uiState.isValid
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Supplier Section
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Supplier Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.supplierGstin,
                        onValueChange = viewModel::onSupplierGstinChange,
                        label = { Text("Supplier GSTIN *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Customer Section
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Customer Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.customerName,
                        onValueChange = viewModel::onCustomerNameChange,
                        label = { Text("Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.customerGstin,
                        onValueChange = viewModel::onCustomerGstinChange,
                        label = { Text("GSTIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.customerAddress,
                        onValueChange = viewModel::onCustomerAddressChange,
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            // Items Section
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Items", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = viewModel::addNewItem) {
                            Icon(Icons.Default.Add, contentDescription = "Add Item")
                        }
                    }

                    uiState.items.forEachIndexed { index, item ->
                        InvoiceItemRow(
                            item = item,
                            onUpdate = { viewModel.updateItem(index, it) },
                            onDelete = { viewModel.removeItem(index) }
                        )
                        if (index < uiState.items.lastIndex) Divider()
                    }
                }
            }

            // Totals Card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Summary", style = MaterialTheme.typography.titleMedium)
                    RowTotal(label = "Subtotal", value = uiState.subtotal)
                    if (uiState.cgstAmount > 0) RowTotal("CGST (9%)", uiState.cgstAmount)
                    if (uiState.sgstAmount > 0) RowTotal("SGST (9%)", uiState.sgstAmount)
                    if (uiState.igstAmount > 0) RowTotal("IGST (18%)", uiState.igstAmount)
                    RowTotal(
                        label = "Total",
                        value = uiState.totalAmount,
                        isBold = true,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Notes & Terms
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            OutlinedTextField(
                value = uiState.termsAndConditions,
                onValueChange = viewModel::onTermsChange,
                label = { Text("Terms & Conditions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

@Composable
private fun RowTotal(
    label: String,
    value: Double,
    isBold: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = style,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "₹${"%.2f".format(value)}",
            style = style,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}