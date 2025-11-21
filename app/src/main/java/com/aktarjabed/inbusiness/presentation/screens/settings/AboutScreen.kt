package com.aktarjabed.inbusiness.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.BuildConfig
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.domain.device.DeviceTier
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val quotaInfo by viewModel.quotaInfo.collectAsState()

    // Auto-detected device info
    val deviceClassifier = remember { DeviceClassifier() }
    val deviceInfo = remember { deviceClassifier.getDeviceInfo(context) }

    var showDeviceDetails by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
        ) {
            // App Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "App Icon",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "INBusiness",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Enterprise-grade invoicing with AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider()

            // Auto-Detected User Info
            AboutSection(title = "Your Account") {
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "User",
                    value = settings.userName.ifBlank { "Not set" }
                )
                InfoRow(
                    icon = Icons.Default.Business,
                    label = "Business",
                    value = settings.businessName.ifBlank { "Not set" }
                )
                InfoRow(
                    icon = Icons.Default.Numbers,
                    label = "GSTIN",
                    value = settings.gstin.ifBlank { "Not set" }
                )

                quotaInfo?.let { quota ->
                    InfoRow(
                        icon = Icons.Default.CardMembership,
                        label = "Subscription",
                        value = quota.tier
                    )
                    InfoRow(
                        icon = Icons.Default.Today,
                        label = "Usage Today",
                        value = "${quota.dailyUsed}/${if (quota.tier == "FREE") quota.dailyLimit else "∞"}"
                    )
                }
            }

            Divider()

            // Auto-Detected Device Info
            AboutSection(title = "Device Information") {
                InfoRow(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Device",
                    value = "${deviceInfo.manufacturer} ${deviceInfo.model}"
                )
                InfoRow(
                    icon = Icons.Default.Android,
                    label = "Android",
                    value = "Android ${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt})"
                )
                InfoRow(
                    icon = Icons.Default.Memory,
                    label = "RAM",
                    value = "${deviceInfo.ramMB / 1024} GB"
                )
                InfoRow(
                    icon = Icons.Default.Speed,
                    label = "CPU Cores",
                    value = "${deviceInfo.cpuCores} cores"
                )
                InfoRow(
                    icon = Icons.Default.Assessment,
                    label = "Device Tier",
                    value = deviceInfo.tier.name.lowercase().capitalize(),
                    valueColor = when (deviceInfo.tier) {
                        DeviceTier.HIGH_END -> MaterialTheme.colorScheme.primary
                        DeviceTier.MID_RANGE -> MaterialTheme.colorScheme.secondary
                        DeviceTier.LOW_END -> MaterialTheme.colorScheme.tertiary
                    }
                )

                TextButton(
                    onClick = { showDeviceDetails = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Technical Details")
                }
            }

            Divider()

            // App Features
            AboutSection(title = "Features") {
                FeatureRow(
                    icon = Icons.Default.Security,
                    title = "Bank-Grade Security",
                    description = "SQLCipher + Android Keystore encryption"
                )
                FeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Anomaly Detection",
                    description = "5 algorithms for fraud prevention",
                    enabled = settings.aiAnomalyDetectionEnabled
                )
                FeatureRow(
                    icon = Icons.Default.Receipt,
                    title = "E-Invoicing (NIC)",
                    description = "Government-compliant IRN generation"
                )
                FeatureRow(
                    icon = Icons.Default.CloudOff,
                    title = "Offline-First",
                    description = "Works without internet connection"
                )
            }

            Divider()

            // Links
            AboutSection(title = "Support & Legal") {
                LinkRow(
                    icon = Icons.Default.Language,
                    label = "Website",
                    url = "https://inbusiness.app",
                    context = context
                )
                LinkRow(
                    icon = Icons.Default.Code,
                    label = "GitHub",
                    url = "https://github.com/aktarjabed/INBusiness",
                    context = context
                )
                LinkRow(
                    icon = Icons.Default.Email,
                    label = "Support",
                    url = "mailto:support@inbusiness.app",
                    context = context
                )
                LinkRow(
                    icon = Icons.Default.Description,
                    label = "Privacy Policy",
                    url = "https://inbusiness.app/privacy",
                    context = context
                )
                LinkRow(
                    icon = Icons.Default.Gavel,
                    label = "Terms of Service",
                    url = "https://inbusiness.app/terms",
                    context = context
                )
            }

            Divider()

            // Build Info
            AboutSection(title = "Build Information") {
                InfoRow(
                    icon = Icons.Default.Code,
                    label = "Build Type",
                    value = if (BuildConfig.DEBUG) "Debug" else "Release"
                )
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Build Date",
                    value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(BuildConfig.BUILD_TIME))
                )
                InfoRow(
                    icon = Icons.Default.Commit,
                    label = "Git Commit",
                    value = BuildConfig.GIT_HASH.take(7)
                )
            }

            // Copyright
            Text(
                text = "© 2025 INBusiness. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    // Device Details Dialog
    if (showDeviceDetails) {
        DeviceDetailsDialog(
            deviceInfo = deviceInfo,
            onDismiss = { showDeviceDetails = false }
        )
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        },
        leadingContent = {
            Icon(icon, label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                icon,
                title,
                tint = if (enabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = if (enabled) {
            { Icon(Icons.Default.CheckCircle, "Enabled", tint = MaterialTheme.colorScheme.primary) }
        } else null
    )
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    url: String,
    context: Context
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(icon, label, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Icon(Icons.Default.OpenInNew, "Open")
        },
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    )
}

@Composable
private fun DeviceDetailsDialog(
    deviceInfo: com.aktarjabed.inbusiness.domain.device.DeviceInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Technical Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailItem("Manufacturer", deviceInfo.manufacturer)
                DetailItem("Model", deviceInfo.model)
                DetailItem("Android Version", deviceInfo.androidVersion)
                DetailItem("SDK Level", deviceInfo.sdkInt.toString())
                DetailItem("RAM", "${deviceInfo.ramMB} MB")
                DetailItem("CPU Cores", deviceInfo.cpuCores.toString())
                DetailItem("Device Tier", deviceInfo.tier.name)
                DetailItem("Build Fingerprint", Build.FINGERPRINT)
                DetailItem("Hardware", Build.HARDWARE)
                DetailItem("Product", Build.PRODUCT)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}