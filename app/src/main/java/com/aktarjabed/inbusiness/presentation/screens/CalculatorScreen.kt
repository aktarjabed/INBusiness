package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.inbusiness.presentation.components.InputField
import com.aktarjabed.inbusiness.presentation.components.MetricCard
import com.aktarjabed.inbusiness.presentation.viewmodel.CalculatorViewModel
import kotlinx.coroutines.flow.collect
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val data by viewModel.businessData.collectAsStateWithLifecycle()
    val metrics by viewModel.financialMetrics.collectAsStateWithLifecycle()
    val scenarios by viewModel.savedScenarios.collectAsStateWithLifecycle()
    val isSaving by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSaveScenarioDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.errorMsg.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Business Calculator") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Costs & Expenses", style = MaterialTheme.typography.titleMedium)

                    NumberInputField(
                        value = data.rawMaterialsCost,
                        onValueChange = { viewModel.updateBusinessData(data.copy(rawMaterialsCost = it)) },
                        label = "Raw Materials (₹)"
                    )
                    NumberInputField(
                        value = data.supplierCosts,
                        onValueChange = { viewModel.updateBusinessData(data.copy(supplierCosts = it)) },
                        label = "Supplier Costs (₹)"
                    )
                    NumberInputField(
                        value = data.inputGst,
                        onValueChange = { viewModel.updateBusinessData(data.copy(inputGst = it)) },
                        label = "Input GST (₹)"
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Revenue", style = MaterialTheme.typography.titleMedium)

                    NumberInputField(
                        value = data.unitPrice,
                        onValueChange = { viewModel.updateBusinessData(data.copy(unitPrice = it)) },
                        label = "Unit Price (₹)"
                    )
                    NumberInputField(
                        value = data.quantity,
                        onValueChange = { viewModel.updateBusinessData(data.copy(quantity = it)) },
                        label = "Quantity"
                    )
                    NumberInputField(
                        value = data.outputGst,
                        onValueChange = { viewModel.updateBusinessData(data.copy(outputGst = it)) },
                        label = "Output GST (₹)"
                    )
                }
            }

            // Results Section
            Text("Financial Metrics", style = MaterialTheme.typography.titleLarge)

            MetricCard("Net Profit", "₹${metrics.netProfit}")
            MetricCard("EBITDA", "₹${metrics.ebitda}")
            MetricCard("Gross Margin", "${metrics.grossMargin}%")
            MetricCard("Net Margin", "${metrics.netMargin}%")
            MetricCard("ROI", "${metrics.roi}%")
            MetricCard("Break Even Point", "${metrics.breakEvenPoint} units")
            MetricCard("GST Payable", "₹${metrics.gstPayable}")
            MetricCard("Cash Flow", "₹${metrics.cashFlow}")

            // Scenarios: previously only reachable through the ViewModel API, never the UI.
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Scenarios", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Save the numbers above as a named scenario to compare options. " +
                            "Scenarios are stored separately from the live business profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showSaveScenarioDialog = true },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save current as scenario")
                    }

                    if (scenarios.isEmpty()) {
                        Text(
                            "No saved scenarios yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        scenarios.forEach { scenario ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        scenario.scenarioName.ifBlank { "Unnamed scenario" },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Unit price ₹${scenario.unitPrice} · Qty ${scenario.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { viewModel.loadScenario(scenario) }) {
                                    Text("Load")
                                }
                                TextButton(onClick = { viewModel.deleteScenario(scenario) }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveScenarioDialog) {
        var scenarioName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveScenarioDialog = false },
            title = { Text("Save scenario") },
            text = {
                OutlinedTextField(
                    value = scenarioName,
                    onValueChange = { scenarioName = it },
                    label = { Text("Scenario name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = scenarioName.isNotBlank() && !isSaving,
                    onClick = {
                        viewModel.saveScenario(scenarioName)
                        showSaveScenarioDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveScenarioDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Numeric input that keeps the raw text locally while it is being typed.
 *
 * Binding a `Double` straight to a text field cannot round-trip partial input: typing
 * "1" of "15" rewrites the field to "1.0", so the next keystroke produces "1.05"
 * instead of "15". This wrapper also requests [KeyboardType.Decimal], because the
 * default [KeyboardType.Number] keypad has no decimal separator on most keyboards and
 * money cannot be entered without one.
 */
@Composable
private fun NumberInputField(
    value: Double,
    onValueChange: (Double) -> Unit,
    label: String
) {
    var text by remember { mutableStateOf(formatNumber(value)) }
    var lastReported by remember { mutableStateOf(value) }

    InputField(
        value = text,
        onValueChange = { input ->
            // Only accept characters that can form a non-negative decimal number.
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                text = input
                val parsed = input.toDoubleOrNull()
                when {
                    parsed != null && parsed.isFinite() -> {
                        lastReported = parsed
                        onValueChange(parsed)
                    }
                    input.isBlank() -> {
                        lastReported = 0.0
                        onValueChange(0.0)
                    }
                }
            }
        },
        label = label,
        keyboardType = KeyboardType.Decimal
    )

    // Adopt external changes (e.g. loading a scenario) without fighting the user's cursor.
    LaunchedEffect(value) {
        if (value != lastReported) {
            lastReported = value
            text = formatNumber(value)
        }
    }
}

private fun formatNumber(value: Double): String {
    if (!value.isFinite() || value == 0.0) return ""
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
