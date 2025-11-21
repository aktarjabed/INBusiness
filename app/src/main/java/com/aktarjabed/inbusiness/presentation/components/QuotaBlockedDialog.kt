package com.aktarjabed.inbusiness.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import java.time.format.DateTimeFormatter

@Composable
fun QuotaBlockedDialog(
    verdict: QuotaVerdict,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (verdict) {
                    is QuotaVerdict.DailyCap -> "Daily Limit Reached"
                    is QuotaVerdict.MonthlyCap -> "Monthly Limit Reached"
                    is QuotaVerdict.FreeExpired -> "Free Trial Expired"
                    else -> "Limit Reached"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (verdict) {
                    is QuotaVerdict.DailyCap -> {
                        Text(
                            text = "You've used all ${verdict.limit} free invoices today.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        val resetTime = verdict.resetTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                        Text(
                            text = "Resets at $resetTime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))
                        UpgradeOptions()
                    }

                    is QuotaVerdict.MonthlyCap -> {
                        Text(
                            text = "You've reached your monthly limit of 60 invoices.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        UpgradeOptions()
                    }

                    is QuotaVerdict.FreeExpired -> {
                        Text(
                            text = "Your free trial has ended after 1 year.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Thank you for using INBusiness! Upgrade to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        UpgradeOptions()
                    }

                    else -> {
                        Text("Upgrade to continue creating invoices")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpgrade) {
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Composable
private fun UpgradeOptions() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Upgrade Options:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        PricingCard(
            tier = "Basic",
            price = "₹99/month",
            features = "Unlimited invoices",
            highlighted = false
        )

        PricingCard(
            tier = "Pro",
            price = "₹149/month",
            features = "AI + Security + IRN",
            highlighted = true
        )

        PricingCard(
            tier = "Enterprise",
            price = "₹499/month",
            features = "Teams + API",
            highlighted = false
        )
    }
}

@Composable
private fun PricingCard(
    tier: String,
    price: String,
    features: String,
    highlighted: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (highlighted)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tier,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = features,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = price,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}