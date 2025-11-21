package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.presentation.components.LoadingScreen
import com.aktarjabed.inbusiness.presentation.components.QuotaBlockedDialog
import com.aktarjabed.inbusiness.presentation.components.QuotaWarningBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    viewModel: InvoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var customerName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }

    // Check quota when screen loads
    LaunchedEffect(Unit) {
        viewModel.checkQuotaAndPrepare()
    }

    // Handle success
    LaunchedEffect(uiState) {
        if (uiState is InvoiceUiState.Success) {
            // Navigate back after 1.5 seconds
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Invoice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InvoiceUiState.Initial,
                is InvoiceUiState.Loading -> {
                    LoadingScreen(message = "Checking quota...")
                }

                is InvoiceUiState.CreateAllowed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quota warning
                        QuotaWarningBanner(
                            remaining = state.remainingToday,
                            onUpgrade = onNavigateToUpgrade
                        )

                        // Invoice number (read-only)
                        OutlinedTextField(
                            value = state.invoiceNumber,
                            onValueChange = {},
                            label = { Text("Invoice Number") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Customer name
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Total amount
                        OutlinedTextField(
                            value = totalAmount,
                            onValueChange = { totalAmount = it },
                            label = { Text("Total Amount (₹)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        // Create button
                        Button(
                            onClick = {
                                val amount = totalAmount.toDoubleOrNull() ?: 0.0
                                if (customerName.isNotBlank() && amount > 0) {
                                    viewModel.createInvoice(customerName, amount)
                                }
                            },
                            enabled = customerName.isNotBlank() && totalAmount.toDoubleOrNull() != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Invoice")
                        }

                        // Quota info
                        Text(
                            text = "Remaining today: ${state.remainingToday} invoices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is InvoiceUiState.QuotaBlocked -> {
                    QuotaBlockedDialog(
                        verdict = state.verdict,
                        onUpgrade = onNavigateToUpgrade,
                        onDismiss = onNavigateBack
                    )
                }

                is InvoiceUiState.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                is InvoiceUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.checkQuotaAndPrepare() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}