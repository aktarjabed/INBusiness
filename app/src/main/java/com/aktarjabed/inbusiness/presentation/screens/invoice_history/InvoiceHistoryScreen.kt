package com.aktarjabed.inbusiness.presentation.screens.invoice_history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.utils.AppDateUtils
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    viewModel: InvoiceHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showFilters by remember { mutableStateOf(false) }

    // Re-read the first page whenever the screen is (re)entered: returning from the preview
    // after cancelling an invoice must not leave the old status on screen.
    LaunchedEffect(Unit) { viewModel.loadInvoices(reset = true) }

    // Load more when scrolling near the end
    LaunchedEffect(listState.layoutInfo) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = listState.layoutInfo.totalItemsCount
        if (lastVisible >= totalItems - 3 && !uiState.isLoading && !uiState.isEndOfList) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Toggle Filters")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by invoice # or customer name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            // Filter panel
            if (showFilters) {
                FilterPanel(
                    currentStatus = uiState.status,
                    currentPaymentStatus = uiState.paymentStatus,
                    currentDocType = uiState.documentType,
                    onStatusChanged = { viewModel.onStatusChanged(it) },
                    onPaymentStatusChanged = { viewModel.onPaymentStatusChanged(it) },
                    onDocTypeChanged = { viewModel.onDocumentTypeChanged(it) }
                )
            }

            // Content
            when {
                uiState.isLoading && uiState.invoices.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.invoices.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Error: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadInvoices(reset = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                uiState.invoices.isEmpty() && !uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No invoices found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.invoices, key = { it.id }) { invoice ->
                            InvoiceHistoryCard(
                                invoice = invoice,
                                onClick = { onNavigateToPreview(invoice.id) }
                            )
                        }

                        // Loading indicator at bottom for pagination
                        if (uiState.isLoading && uiState.invoices.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        // End of list indicator
                        if (uiState.isEndOfList && uiState.invoices.isNotEmpty()) {
                            item {
                                Text(
                                    "All invoices loaded",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    currentStatus: String?,
    currentPaymentStatus: String?,
    currentDocType: String?,
    onStatusChanged: (String?) -> Unit,
    onPaymentStatusChanged: (String?) -> Unit,
    onDocTypeChanged: (String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Status",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentStatus == null,
                    onClick = { onStatusChanged(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = currentStatus == "COMPLETED",
                    onClick = { onStatusChanged("COMPLETED") },
                    label = { Text("Completed") }
                )
                FilterChip(
                    selected = currentStatus == "CANCELLED",
                    onClick = { onStatusChanged("CANCELLED") },
                    label = { Text("Cancelled") }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                "Payment",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentPaymentStatus == null,
                    onClick = { onPaymentStatusChanged(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = currentPaymentStatus == "PAID",
                    onClick = { onPaymentStatusChanged("PAID") },
                    label = { Text("Paid") }
                )
                FilterChip(
                    selected = currentPaymentStatus == "UNPAID",
                    onClick = { onPaymentStatusChanged("UNPAID") },
                    label = { Text("Unpaid") }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                "Document Type",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentDocType == null,
                    onClick = { onDocTypeChanged(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = currentDocType == "TAX_INVOICE",
                    onClick = { onDocTypeChanged("TAX_INVOICE") },
                    label = { Text("Tax Invoice") }
                )
                FilterChip(
                    selected = currentDocType == "BILL_OF_SUPPLY",
                    onClick = { onDocTypeChanged("BILL_OF_SUPPLY") },
                    label = { Text("Bill of Supply") }
                )
            }
        }
    }
}

@Composable
private fun InvoiceHistoryCard(
    invoice: Invoice,
    onClick: () -> Unit
) {
    // Ledger timestamps are displayed in the authoritative business timezone, matching the
    // PDF, the preview and the dashboard's day windows.
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(AppDateUtils.businessZoneId)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (invoice.documentType.isNotBlank()) {
                        DocumentTypeChip(type = invoice.documentType)
                    }
                    StatusChip(status = invoice.status)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = invoice.customerName,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!invoice.customerGSTIN.isNullOrBlank()) {
                Text(
                    text = "GSTIN: ${invoice.customerGSTIN}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormatter.format(invoice.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (invoice.supplyType.isNotBlank()) {
                    Text(
                        text = invoice.supplyType.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format(Locale.US, "₹%.2f", invoice.totalAmount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Paid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format(Locale.US, "₹%.2f", invoice.amountPaid),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format(Locale.US, "₹%.2f", invoice.balanceDue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (invoice.balanceDue > 0) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (color, textColor) = when (status) {
        "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DocumentTypeChip(type: String) {
    val label = when (type) {
        "TAX_INVOICE" -> "TAX"
        "BILL_OF_SUPPLY" -> "BOS"
        else -> type.replace("_", " ")
    }
    val (color, textColor) = when (type) {
        "TAX_INVOICE" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "BILL_OF_SUPPLY" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}