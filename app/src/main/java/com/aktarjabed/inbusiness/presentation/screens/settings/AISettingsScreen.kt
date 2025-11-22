package com.aktarjabed.inbusiness.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.settings.AISensitivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.userSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "AI Anomaly Detection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "AI analyzes your invoices in real-time to detect unusual patterns, potential errors, and fraud risks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Sensitivity Selector
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detection Sensitivity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    AISensitivity.values().forEach { sensitivity ->
                        SensitivityOption(
                            sensitivity = sensitivity,
                            selected = settings.aiSensitivity == sensitivity,
                            onSelect = {
                                viewModel.updateAISettings(
                                    enabled = settings.aiAnomalyDetectionEnabled,
                                    sensitivity = sensitivity,
                                    autoCorrect = settings.aiAutoCorrectEnabled,
                                    suggestions = settings.aiSuggestionsEnabled
                                )
                            }
                        )
                    }
                }
            }

            // Auto-Correct
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Correct",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically fix detected errors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.aiAutoCorrectEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateAISettings(
                                    enabled = settings.aiAnomalyDetectionEnabled,
                                    sensitivity = settings.aiSensitivity,
                                    autoCorrect = enabled,
                                    suggestions = settings.aiSuggestionsEnabled
                                )
                            }
                        )
                    }
                }
            }

            // What AI Detects
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What AI Detects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    DetectionItem(
                        title = "Statistical Outliers",
                        description = "Unusually high or low invoice amounts"
                    )
                    DetectionItem(
                        title = "GST Compliance",
                        description = "Incorrect tax rates or GSTIN formats"
                    )
                    DetectionItem(
                        title = "Duplicate Invoices",
                        description = "Potential double-billing"
                    )
                    DetectionItem(
                        title = "Pricing Anomalies",
                        description = "Sudden price changes for items"
                    )
                    DetectionItem(
                        title = "Temporal Patterns",
                        description = "Unusual transaction times"
                    )
                }
            }
        }
    }
}

@Composable
private fun SensitivityOption(
    sensitivity: AISensitivity,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val (label, description) = when (sensitivity) {
        AISensitivity.LOW -> "Low" to "Fewer warnings, critical issues only"
        AISensitivity.MEDIUM -> "Medium" to "Balanced detection (recommended)"
        AISensitivity.HIGH -> "High" to "Maximum protection, more alerts"
    }

    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DetectionItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}