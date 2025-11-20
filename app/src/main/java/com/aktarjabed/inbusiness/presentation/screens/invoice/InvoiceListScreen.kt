package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceStatus
import com.aktarjabed.inbusiness.utils.toLocalDate
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onNewInvoice: () -> Unit,
    onEditInvoice: (String) -> Unit,
    onPreviewPdf: (String) -> Unit,
    viewModel: InvoiceListViewModel = hiltViewModel()
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Invoices") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewInvoice) {
                Icon(Icons.Default.Add, contentDescription = "New Invoice")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            AnimatedVisibility(visible = uiState.isSearchVisible) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearch,
                    placeholder = { Text("Search by customer, invoice #") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.onSearch("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                )
            }

            // Filter Chips
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InvoiceStatus.values().forEach { status ->
                    FilterChip(
                        selected = uiState.selectedStatus == status,
                        onClick = { viewModel.onStatusFilter(status) },
                        label = { Text(status.name.replace('_', ' ')) }
                    )
                }
            }

            // Invoice List
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(
                    items = uiState.invoices,
                    key = { it.id }
                ) { invoice ->
                    InvoiceItemCard(
                        invoice = invoice,
                        onEdit = { onEditInvoice(invoice.id) },
                        onPreview = { onPreviewPdf(invoice.id) },
                        onDelete = { viewModel.deleteInvoice(invoice) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceItemCard(
    invoice: Invoice,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = invoice.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "₹${"%.2f".format(invoice.totalAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Due: ${invoice.dueDate.toLocalDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (invoice.balanceAmount > 0 && invoice.dueDate.isBefore(Instant.now()))
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Preview PDF") },
                        onClick = { onPreview(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { onEdit(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}