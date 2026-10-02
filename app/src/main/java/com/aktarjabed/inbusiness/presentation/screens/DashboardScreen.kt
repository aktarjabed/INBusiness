package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.aktarjabed.inbusiness.presentation.components.MetricCard
import com.aktarjabed.inbusiness.presentation.screens.dashboard.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToCalculator: () -> Unit,
    onNavigateToInvoice: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToHistory: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The metric cards are live SQL flows, but the seven-day chart is loaded imperatively.
    // Re-run the load whenever the screen is (re)entered so an invoice created in another
    // screen is reflected on return instead of leaving a stale chart behind.
    LaunchedEffect(Unit) { viewModel.refreshData() }

    // Build Vico chart model from chart data
    val chartModelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(state.chartData) {
        if (state.chartData.isNotEmpty()) {
            chartModelProducer.runTransaction {
                columnSeries { series(state.chartData.map { it.revenue }) }
            }
        }
    }

    // X-axis formatter: index → day abbreviation (e.g. "Mon")
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE") }
    val xFormatter = remember(state.chartData) {
        CartesianValueFormatter { context, x, _ ->
            val index = x.toInt()
            if (index in state.chartData.indices) {
                state.chartData[index].date.format(dayFormatter)
            } else ""
        }
    }

    // Y-axis formatter: value → "₹1.5K" or "₹2L" style
    val yFormatter = remember {
        CartesianValueFormatter { _, y, _ ->
            val v = y
            when {
                v >= 1_00_000 -> "₹${String.format("%.1f", v / 1_00_000)}L"
                v >= 1_000 -> "₹${String.format("%.1f", v / 1_000)}K"
                else -> "₹${String.format("%.0f", v)}"
            }
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
        }

        if (state.isLoading) {
            item {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            item {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.refreshData() }) { Text("Retry") }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Today's Revenue",
                        value = "₹${"%.2f".format(state.todayRevenue)}",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        title = "Total Revenue",
                        value = "₹${"%.2f".format(state.totalRevenue)}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Pending Dues",
                        value = "₹${"%.2f".format(state.pendingDues)}",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        title = "Invoices Today",
                        value = "${state.invoicesToday}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Active Products",
                        value = "${state.activeProducts}",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        title = "Low Stock",
                        value = "${state.lowStockCount}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // 7-day revenue bar chart using Vico
            item {
                Text("7-Day Revenue", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (state.chartData.isNotEmpty()) {
                    val columnLayer = rememberColumnCartesianLayer()
                    val bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = xFormatter,
                        tick = null,
                        guideline = null
                    )
                    val startAxis = VerticalAxis.rememberStart(
                        valueFormatter = yFormatter,
                        tick = null,
                        guideline = rememberAxisGuidelineComponent(fill(Color.Gray.copy(alpha = 0.2f)))
                    )

                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            columnLayer,
                            startAxis = startAxis,
                            bottomAxis = bottomAxis
                        ),
                        modelProducer = chartModelProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                } else {
                    Text(
                        "No revenue data for the past 7 days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        item {
            Button(
                onClick = onNavigateToInvoice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Invoice")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Invoice History")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToInventory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inventory")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToCalculator,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Calculator")
            }
        }
    }
}
