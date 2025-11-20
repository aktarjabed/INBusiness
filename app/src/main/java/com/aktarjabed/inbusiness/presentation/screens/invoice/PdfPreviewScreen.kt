package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.inbusiness.data.entities.InvoiceStatus
import com.aktarjabed.inbusiness.utils.PdfViewer
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    invoiceId: String,
    onBack: () -> Unit,
    viewModel: PdfPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(invoiceId) {
        viewModel.load(invoiceId)
    }

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
                title = { Text("Invoice Preview") },
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
                        onClick = { uiState.pdfFile?.let { viewModel.sharePdf(context, it) } },
                        enabled = uiState.pdfFile != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(
                        onClick = { uiState.pdfFile?.let { viewModel.printPdf(context, it) } },
                        enabled = uiState.pdfFile != null
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                    IconButton(
                        onClick = { viewModel.markAsPaid() },
                        enabled = uiState.invoice?.status != InvoiceStatus.PAID
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Mark Paid")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.pdfFile != null -> PdfViewer(uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", uiState.pdfFile!!), modifier = Modifier.fillMaxSize())
                else -> Text(
                    "No PDF available",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}