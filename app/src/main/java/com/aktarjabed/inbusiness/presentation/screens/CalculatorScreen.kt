package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.components.InputField
import com.aktarjabed.inbusiness.presentation.components.MetricCard
import com.aktarjabed.inbusiness.presentation.viewmodel.CalculatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val data by viewModel.businessData.collectAsState()
    val metrics by viewModel.financialMetrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Collect error messages
    LaunchedEffect(Unit) {
        viewModel.errorMsg.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("INBusiness Calculator") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Purchase & COGS Section
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Purchase & COGS", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    InputField(
                        value = data.rawMaterialsCost.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(rawMaterialsCost = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Raw Materials Cost (₹)"
                    )
                    InputField(
                        value = data.supplierCosts.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(supplierCosts = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Supplier Costs (₹)"
                    )
                    InputField(
                        value = data.inputGst.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(inputGst = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Input GST (₹)"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Sales & Revenue Section
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sales & Revenue", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    InputField(
                        value = data.unitPrice.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(unitPrice = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Unit Price (₹)"
                    )
                    InputField(
                        value = data.quantity.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(quantity = it.toIntOrNull() ?: 0)
                            )
                        },
                        label = "Quantity"
                    )
                    InputField(
                        value = data.outputGst.toString(),
                        onValueChange = {
                            viewModel.updateBusinessData(
                                data.copy(outputGst = it.toDoubleOrNull() ?: 0.0)
                            )
                        },
                        label = "Output GST (₹)"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Results Section
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Results", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = "Gross Profit",
                            value = "₹${"%.2f".format(metrics.grossProfit)}",
                            subtitle = "${"%.1f".format(metrics.grossMargin)}% margin",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Net Profit",
                            value = "₹${"%.2f".format(metrics.netProfit)}",
                            subtitle = "${"%.1f".format(metrics.netMargin)}% margin",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = "GST Payable",
                            value = "₹${"%.2f".format(metrics.gstPayable)}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Break-even",
                            value = "%.0f".format(metrics.breakEvenPoint),
                            subtitle = "units",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveScenario("Quick Save") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && metrics.revenue > 0
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Scenario")
                }
            }
        }
    }
}