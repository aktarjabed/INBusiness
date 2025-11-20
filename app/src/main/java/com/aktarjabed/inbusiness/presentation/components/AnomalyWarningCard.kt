package com.aktarjabed.inbusiness.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aktarjabed.inbusiness.domain.models.Anomaly

@Composable
fun AnomalyWarningCard(
    anomalies: List<Anomaly>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (anomalies.isEmpty()) return

    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = when (anomalies.maxOf { it.severity }) {
                Anomaly.Severity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                Anomaly.Severity.HIGH -> MaterialTheme.colorScheme.errorContainer
                Anomaly.Severity.MEDIUM -> Color(0xFFFF9800).copy(alpha = 0.1f)
                Anomaly.Severity.LOW -> Color(0xFFFFC107).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(anomalies.first().getColor())
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${anomalies.size} Anomal${if (anomalies.size > 1) "ies" else "y"} Detected",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }
            }

            // Details (when expanded)
            if (expanded) {
                Spacer(Modifier.height(8.dp))

                anomalies.forEach { anomaly ->
                    AnomalyItem(anomaly)
                    if (anomaly != anomalies.last()) {
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnomalyItem(anomaly: Anomaly) {
    Column {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = when (anomaly.severity) {
                    Anomaly.Severity.CRITICAL -> Icons.Default.Error
                    Anomaly.Severity.HIGH -> Icons.Default.Warning
                    Anomaly.Severity.MEDIUM -> Icons.Default.Info
                    Anomaly.Severity.LOW -> Icons.Default.Info
                },
                contentDescription = null,
                tint = Color(anomaly.getColor()),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = anomaly.title,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = anomaly.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                if (anomaly.suggestion.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = anomaly.suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}