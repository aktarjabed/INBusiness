package com.aktarjabed.inbusiness.presentation.screens.invoice_preview

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.presentation.components.LoadingScreen
import com.aktarjabed.inbusiness.utils.AppDateUtils
import com.aktarjabed.inbusiness.utils.pdf.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: InvoicePreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recordPaymentState by viewModel.recordPaymentState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmountText by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var paymentModeMenuExpanded by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(recordPaymentState) {
        if (recordPaymentState is RecordPaymentUiState.Saved) {
            showPaymentDialog = false
            paymentAmountText = ""
            snackbarHostState.showSnackbar("Payment recorded.")
            viewModel.resetRecordPaymentState()
        }
    }

    val parsedPaymentAmount = paymentAmountText.toDoubleOrNull()
    val paymentInputValid = parsedPaymentAmount != null && parsedPaymentAmount.isFinite() && parsedPaymentAmount > 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Preview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val success = uiState as? InvoicePreviewUiState.Success
            if (success != null && success.invoice.status != "CANCELLED") {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (success.invoice.status == "COMPLETED" && success.invoice.balanceDue > 0.0) {
                            OutlinedButton(
                                onClick = {
                                    paymentAmountText = String.format(Locale.US, "%.2f", success.invoice.balanceDue)
                                    showPaymentDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Record Payment")
                            }
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            PdfGenerator(context).generateInvoicePdf(success.invoice, success.items)
                                        }
                                        if (file == null) {
                                            snackbarHostState.showSnackbar("Could not create the invoice PDF.")
                                        } else {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Invoice"))
                                        }
                                    } catch (e: Exception) {
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        snackbarHostState.showSnackbar("Could not share the invoice: ${e.message ?: "unknown error"}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.padding(end = 8.dp))
                                Text("Share PDF")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InvoicePreviewUiState.Loading -> LoadingScreen(message = "Loading invoice...")
                is InvoicePreviewUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) { Text("Go Back") }
                    }
                }
                is InvoicePreviewUiState.Success -> {
                    InvoiceDetails(invoice = state.invoice, items = state.items)
                }
            }
        }
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = {
                if (recordPaymentState !is RecordPaymentUiState.Saving) {
                    showPaymentDialog = false
                    viewModel.resetRecordPaymentState()
                }
            },
            title = { Text("Record a payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Amount received (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = paymentAmountText.isNotBlank() && !paymentInputValid
                    )
                    ExposedDropdownMenuBox(
                        expanded = paymentModeMenuExpanded,
                        onExpandedChange = { paymentModeMenuExpanded = !paymentModeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = paymentMode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentModeMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = paymentModeMenuExpanded,
                            onDismissRequest = { paymentModeMenuExpanded = false }
                        ) {
                            listOf("CASH", "CARD", "UPI", "BANK_TRANSFER").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMode = method
                                        paymentModeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (recordPaymentState is RecordPaymentUiState.Error) {
                        Text(
                            (recordPaymentState as RecordPaymentUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (recordPaymentState is RecordPaymentUiState.Saving) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = paymentInputValid && recordPaymentState !is RecordPaymentUiState.Saving,
                    onClick = { parsedPaymentAmount?.let { viewModel.recordPayment(it, paymentMode) } }
                ) {
                    Text("Save payment")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = recordPaymentState !is RecordPaymentUiState.Saving,
                    onClick = {
                        showPaymentDialog = false
                        viewModel.resetRecordPaymentState()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InvoiceDetails(invoice: Invoice, items: List<InvoiceItem>) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(AppDateUtils.businessZoneId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Invoice #${invoice.invoiceNumber}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Date: ${dateFormatter.format(invoice.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Supply Type: ${invoice.supplyType.replace("_", " ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (invoice.status == "CANCELLED") {
                    Text(
                        text = "CANCELLED — this invoice is not valid for payment.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Billed To", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Name: ${invoice.customerName}", style = MaterialTheme.typography.bodyLarge)
                if (!invoice.customerGSTIN.isNullOrBlank()) {
                    Text("GSTIN: ${invoice.customerGSTIN}", style = MaterialTheme.typography.bodyMedium)
                }
                if (invoice.buyerAddress.isNotBlank()) {
                    Text("Address: ${invoice.buyerAddress}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Line Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${index + 1}.", modifier = Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.description, fontWeight = FontWeight.Medium)
                            Text("${item.quantity} ${item.unitType} x ₹${item.pricePerUnit} (+ ${item.gstPercentage}% GST)", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(String.format(Locale.US, "₹%.2f", item.totalAmount), fontWeight = FontWeight.Bold)
                    }
                    if (index < items.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                SummaryRow("Subtotal:", invoice.subtotal)
                if (invoice.totalCgst > 0) SummaryRow("CGST:", invoice.totalCgst)
                if (invoice.totalSgst > 0) SummaryRow("SGST:", invoice.totalSgst)
                if (invoice.totalIgst > 0) SummaryRow("IGST:", invoice.totalIgst)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.US, "₹%.2f", invoice.totalAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                SummaryRow("Total paid:", invoice.amountPaid)
                SummaryRow("Balance due:", invoice.balanceDue)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(String.format(Locale.US, "₹%.2f", amount))
    }
}
