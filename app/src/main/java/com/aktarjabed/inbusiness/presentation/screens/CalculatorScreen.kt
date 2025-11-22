package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.model.CalculationResults
import com.aktarjabed.inbusiness.domain.model.IndustryComparison
import com.aktarjabed.inbusiness.presentation.viewmodel.CalculatorViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Calculator Pro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    Row {
                        IconButton(onClick = { viewModel.toggleAdvancedInputs() }) {
                            Icon(
                                if (uiState.showAdvancedInputs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "Toggle Advanced"
                            )
                        }
                        IconButton(onClick = { viewModel.resetCalculator() }) {
                            Icon(Icons.Default.Refresh, "Reset")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.results != null) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Calculate, "Basic") },
                        label = { Text("Basic") },
                        selected = uiState.activeTab == 0,
                        onClick = { /* TODO */ }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Analytics, "Advanced") },
                        label = { Text("Advanced") },
                        selected = uiState.activeTab == 1,
                        onClick = { /* TODO */ }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Assessment, "Ratios") },
                        label = { Text("Ratios") },
                        selected = uiState.activeTab == 2,
                        onClick = { /* TODO */ }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Quick Mode Selector
            QuickModeSelector { viewModel.setQuickMode(it) }

            Spacer(Modifier.height(16.dp))

            // Basic Inputs (Always Visible)
            BasicInputsSection(viewModel)

            // Advanced Inputs Toggle
            if (uiState.showAdvancedInputs) {
                Spacer(Modifier.height(24.dp))
                AdvancedInputsSection(viewModel)
            }

            // Results Section
            uiState.results?.let { results ->
                Spacer(Modifier.height(32.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                ResultsSection(results, currencyFormatter)
            }
        }
    }
}

@Composable
private fun QuickModeSelector(onSelect: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Start Templates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { onSelect("RETAIL_SHOP") },
                    label = { Text("Retail Shop") }
                )
                FilterChip(
                    selected = false,
                    onClick = { onSelect("CONSULTING") },
                    label = { Text("Consulting") }
                )
                FilterChip(
                    selected = false,
                    onClick = { onSelect("MANUFACTURING") },
                    label = { Text("Manufacturing") }
                )
            }
        }
    }
}

@Composable
private fun BasicInputsSection(viewModel: CalculatorViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    SectionHeader("Business Information")
    BusinessTypeSelector(
        selected = uiState.input.businessType,
        onSelect = { viewModel.updateBusinessType(it) }
    )

    Spacer(Modifier.height(16.dp))

    SectionHeader("Revenue & Costs")
    Row(modifier = Modifier.fillMaxWidth()) {
        InputField(
            label = "Total Revenue (₹)",
            value = uiState.input.totalRevenue.toString(),
            onValueChange = { viewModel.updateRevenue(it) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        InputField(
            label = "COGS (₹)",
            value = uiState.input.cogs.toString(),
            onValueChange = { viewModel.updateCOGS(it) },
            modifier = Modifier.weight(1f)
        )
    }

    InputField(
        label = "Operating Expenses (₹)",
        value = uiState.input.operatingExpenses.toString(),
        onValueChange = { viewModel.updateOperatingExpenses(it) }
    )

    // Optional basic inputs
    Row(modifier = Modifier.fillMaxWidth()) {
        InputField(
            label = "Depreciation (₹)",
            value = uiState.input.depreciation.toString(),
            onValueChange = { viewModel.updateDepreciation(it) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        InputField(
            label = "Interest (₹)",
            value = uiState.input.interestExpense.toString(),
            onValueChange = { viewModel.updateInterestExpense(it) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdvancedInputsSection(viewModel: CalculatorViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    SectionHeader("Advanced Financial Inputs")

    // Capital Expenditure
    CollapsibleSection("Capital Expenditure") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "CapEx (₹)",
                value = uiState.input.capEx.toString(),
                onValueChange = { viewModel.updateCapEx(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Asset Sale (₹)",
                value = uiState.input.assetSale.toString(),
                onValueChange = { viewModel.updateAssetSale(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Working Capital
    CollapsibleSection("Working Capital") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "Accounts Receivable (₹)",
                value = uiState.input.accountsReceivable.toString(),
                onValueChange = { viewModel.updateAccountsReceivable(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Inventory (₹)",
                value = uiState.input.inventory.toString(),
                onValueChange = { viewModel.updateInventory(it) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "Accounts Payable (₹)",
                value = uiState.input.accountsPayable.toString(),
                onValueChange = { viewModel.updateAccountsPayable(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Prepaid Expenses (₹)",
                value = uiState.input.prepaidExpenses.toString(),
                onValueChange = { viewModel.updatePrepaidExpenses(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Financing Activities
    CollapsibleSection("Financing Activities") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "Equity Raised (₹)",
                value = uiState.input.equityRaised.toString(),
                onValueChange = { viewModel.updateEquityRaised(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Loan Received (₹)",
                value = uiState.input.loanReceived.toString(),
                onValueChange = { viewModel.updateLoanReceived(it) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "Loan Repayment (₹)",
                value = uiState.input.loanRepayment.toString(),
                onValueChange = { viewModel.updateLoanRepayment(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Dividends Paid (₹)",
                value = uiState.input.dividendsPaid.toString(),
                onValueChange = { viewModel.updateDividendsPaid(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Break-even Analysis
    CollapsibleSection("Break-even Analysis") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "Units Sold",
                value = uiState.input.unitsSold.toString(),
                onValueChange = { viewModel.updateUnitsSold(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Expected Units",
                value = uiState.input.expectedUnits.toString(),
                onValueChange = { viewModel.updateExpectedUnits(it) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "Selling Price/Unit (₹)",
                value = uiState.input.sellingPricePerUnitDetailed.toString(),
                onValueChange = { viewModel.updateSellingPricePerUnitDetailed(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Variable Cost/Unit (₹)",
                value = uiState.input.variableCostPerUnitDetailed.toString(),
                onValueChange = { viewModel.updateVariableCostPerUnitDetailed(it) },
                modifier = Modifier.weight(1f)
            )
        }
        InputField(
            label = "Fixed Costs (₹)",
            value = uiState.input.fixedCostsDetailed.toString(),
            onValueChange = { viewModel.updateFixedCostsDetailed(it) }
        )
    }

    // Business Metadata
    CollapsibleSection("Business Details") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InputField(
                label = "Business Age (months)",
                value = uiState.input.businessAgeMonths.toString(),
                onValueChange = { viewModel.updateBusinessAgeMonths(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            InputField(
                label = "Employees",
                value = uiState.input.numberOfEmployees.toString(),
                onValueChange = { viewModel.updateNumberOfEmployees(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun ResultsSection(
    results: CalculationResults,
    currencyFormatter: NumberFormat
) {
    Text(
        text = "Financial Analysis Results",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(16.dp))

    // Business Health Card (Prominent)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                results.businessHealthScore >= 80 -> MaterialTheme.colorScheme.primary
                results.businessHealthScore >= 60 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Business Health Score",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "%.1f/100".format(results.businessHealthScore),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Risk Level: ${results.riskLevel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // Profitability Metrics
    SectionHeader("Profitability Metrics")

    MetricCard(
        title = "Gross Profit",
        value = currencyFormatter.format(results.grossProfit),
        subtitle = "Margin: ${"%.2f".format(results.grossProfitMargin)}%",
        icon = Icons.Default.TrendingUp,
        color = MaterialTheme.colorScheme.primary
    )

    MetricCard(
        title = "EBITDA",
        value = currencyFormatter.format(results.ebitda),
        subtitle = "Earnings before Interest, Tax, D&A",
        icon = Icons.Default.Analytics
    )

    MetricCard(
        title = "Net Profit",
        value = currencyFormatter.format(results.netProfit),
        subtitle = "Margin: ${"%.2f".format(results.netProfitMargin)}%",
        icon = Icons.Default.AccountBalance,
        color = if (results.netProfit > 0)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.error
    )

    Spacer(Modifier.height(16.dp))

    // Tax & GST
    SectionHeader("Tax & GST Analysis")

    MetricCard(
        title = "Income Tax",
        value = currencyFormatter.format(results.taxCalculation.totalTax),
        subtitle = "Effective Rate: ${"%.2f".format(results.taxCalculation.effectiveRate)}%",
        icon = Icons.Default.Receipt
    )

    MetricCard(
        title = "GST Payable",
        value = currencyFormatter.format(results.gstBreakdown.gstPayable),
        subtitle = "Output: ${currencyFormatter.format(results.gstBreakdown.outputGST)} | Credit: ${currencyFormatter.format(results.gstBreakdown.inputCredit)}",
        icon = Icons.Default.Money
    )

    Spacer(Modifier.height(16.dp))

    // Cash Flow
    SectionHeader("Cash Flow Analysis")

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniMetricCard(
            title = "Operating",
            value = currencyFormatter.format(results.operatingCashFlow),
            modifier = Modifier.weight(1f)
        )
        MiniMetricCard(
            title = "Investing",
            value = currencyFormatter.format(results.investingCashFlow),
            modifier = Modifier.weight(1f)
        )
        MiniMetricCard(
            title = "Financing",
            value = currencyFormatter.format(results.financingCashFlow),
            modifier = Modifier.weight(1f)
        )
    }

    MetricCard(
        title = "Net Cash Flow",
        value = currencyFormatter.format(results.netCashFlow),
        subtitle = "Total cash generated/used",
        icon = Icons.Default.AccountBalanceWallet,
        color = if (results.netCashFlow > 0)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.error
    )

    Spacer(Modifier.height(16.dp))

    // Break-even Analysis
    SectionHeader("Break-even Analysis")

    MetricCard(
        title = "Break-even Point",
        value = "${results.breakEvenUnits} units",
        subtitle = "Revenue: ${currencyFormatter.format(results.breakEvenRevenue)}",
        icon = Icons.Default.ShowChart
    )

    MetricCard(
        title = "Margin of Safety",
        value = "${"%.2f".format(results.marginOfSafety)}%",
        subtitle = "Buffer above break-even",
        icon = Icons.Default.Security
    )

    Spacer(Modifier.height(16.dp))

    // Financial Ratios
    SectionHeader("Financial Ratios")

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RatioCard(
            title = "Current Ratio",
            value = "%.2f".format(results.currentRatio),
            target = "2.0",
            isGood = results.currentRatio >= 1.5,
            modifier = Modifier.weight(1f)
        )
        RatioCard(
            title = "Debt/Equity",
            value = "%.2f".format(results.debtToEquity),
            target = "<1.0",
            isGood = results.debtToEquity < 1.0,
            modifier = Modifier.weight(1f)
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RatioCard(
            title = "ROE",
            value = "${"%.1f".format(results.roe)}%",
            target = "15%",
            isGood = results.roe >= 15.0,
            modifier = Modifier.weight(1f)
        )
        RatioCard(
            title = "ROA",
            value = "${"%.1f".format(results.roa)}%",
            target = "10%",
            isGood = results.roa >= 10.0,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(16.dp))

    // Industry Comparison
    SectionHeader("Industry Benchmark Comparison")

    IndustryComparisonCard(results.industryComparison)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BusinessTypeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("INDIVIDUAL", "COMPANY", "LLP", "PARTNERSHIP").forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type) }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Info,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = color
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun MiniMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun RatioCard(
    title: String,
    value: String,
    target: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isGood)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGood)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Target: $target",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IndustryComparisonCard(comparison: IndustryComparison) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Industry: ${comparison.industryType}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Score: ${comparison.performanceScore.toInt()}/100",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        comparison.performanceScore >= 80 -> MaterialTheme.colorScheme.primary
                        comparison.performanceScore >= 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            ComparisonRow(
                metric = "Gross Margin",
                yourValue = "${"%.1f".format(comparison.yourGrossMargin)}%",
                industryValue = "${"%.1f".format(comparison.industryGrossMargin)}%",
                isGood = comparison.yourGrossMargin >= comparison.industryGrossMargin
            )

            ComparisonRow(
                metric = "Net Margin",
                yourValue = "${"%.1f".format(comparison.yourNetMargin)}%",
                industryValue = "${"%.1f".format(comparison.industryNetMargin)}%",
                isGood = comparison.yourNetMargin >= comparison.industryNetMargin
            )

            ComparisonRow(
                metric = "Current Ratio",
                yourValue = "%.2f".format(comparison.yourCurrentRatio),
                industryValue = "%.2f".format(comparison.industryCurrentRatio),
                isGood = comparison.yourCurrentRatio >= comparison.industryCurrentRatio
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    metric: String,
    yourValue: String,
    industryValue: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Industry Avg: $industryValue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = yourValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isGood)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (isGood) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (isGood)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}