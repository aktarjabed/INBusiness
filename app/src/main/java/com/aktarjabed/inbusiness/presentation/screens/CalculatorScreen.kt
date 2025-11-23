package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.model.BusinessInput
import com.aktarjabed.inbusiness.domain.model.CalculationResults
import com.aktarjabed.inbusiness.presentation.viewmodel.CalculatorViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val inputs by viewModel.inputs.collectAsState()
    val results by viewModel.results.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Business Planner",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- RESULTS DASHBOARD ---
        ResultDashboard(results)

        Spacer(modifier = Modifier.height(24.dp))

        // --- INPUT SECTIONS ---
        CollapsibleSection("1. Revenue Model", true) {
            NumberInput("Avg Order Value (₹)", inputs.averageOrderValue) {
                viewModel.updateInput { s -> s.copy(averageOrderValue = it) }
            }
            NumberInput("Monthly Orders", inputs.monthlyOrders) {
                viewModel.updateInput { s -> s.copy(monthlyOrders = it) }
            }
            NumberInput("Annual Growth (%)", inputs.growthRate) {
                viewModel.updateInput { s -> s.copy(growthRate = it) }
            }
        }

        CollapsibleSection("2. Variable Costs (Per Unit)") {
            NumberInput("Raw Material", inputs.materialCostPerUnit) {
                viewModel.updateInput { s -> s.copy(materialCostPerUnit = it) }
            }
            NumberInput("Packaging", inputs.packagingCost) {
                viewModel.updateInput { s -> s.copy(packagingCost = it) }
            }
            NumberInput("Shipping/Delivery", inputs.shippingCost) {
                viewModel.updateInput { s -> s.copy(shippingCost = it) }
            }
        }

        CollapsibleSection("3. Monthly Fixed Costs (OpEx)") {
            NumberInput("Office Rent", inputs.rent) {
                viewModel.updateInput { s -> s.copy(rent = it) }
            }
            NumberInput("Staff Salaries", inputs.salaries) {
                viewModel.updateInput { s -> s.copy(salaries = it) }
            }
            NumberInput("Marketing / Ads", inputs.marketingBudget) {
                viewModel.updateInput { s -> s.copy(marketingBudget = it) }
            }
            NumberInput("Software / SaaS", inputs.softwareSubscriptions) {
                viewModel.updateInput { s -> s.copy(softwareSubscriptions = it) }
            }
        }

        CollapsibleSection("4. One-time Investment (CapEx)") {
            NumberInput("Equipment / Machinery", inputs.equipmentCost) {
                viewModel.updateInput { s -> s.copy(equipmentCost = it) }
            }
            NumberInput("Licenses & Legal", inputs.licenseFees) {
                viewModel.updateInput { s -> s.copy(licenseFees = it) }
            }
            NumberInput("Initial Inventory", inputs.initialInventory) {
                viewModel.updateInput { s -> s.copy(initialInventory = it) }
            }
        }

        CollapsibleSection("5. Taxes & Compliance") {
            NumberInput("GST Rate (%)", inputs.gstRate) {
                viewModel.updateInput { s -> s.copy(gstRate = it) }
            }
            NumberInput("Income Tax Rate (%)", inputs.incomeTaxRate) {
                viewModel.updateInput { s -> s.copy(incomeTaxRate = it) }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ResultDashboard(res: CalculationResults) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Net Profit", style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatCurrency(res.netProfitAfterTax),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (res.netProfitAfterTax >= 0) Color(0xFF006400) else Color.Red
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Margin", String.format("%.1f%%", res.netMarginPercent))
                MetricItem("Break-even", "${res.breakEvenUnits} units")
                MetricItem("ROI", "${String.format("%.1f", res.roiMonths)} mo")
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun NumberInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onValueChange(it) },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
}
