package com.aktarjabed.inbusiness.presentation.screens.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.settings.AISensitivity
import com.aktarjabed.inbusiness.domain.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToAI: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.userSettings.collectAsState()
    val quotaInfo by viewModel.quotaInfo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Quota Card (Top)
            quotaInfo?.let { quota ->
                QuotaCard(
                    quotaInfo = quota,
                    onUpgrade = onNavigateToUpgrade,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Divider()

            // Profile Section
            SettingsSection(title = "Profile") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account",
                    subtitle = settings.userName.ifBlank { "Set up your profile" },
                    onClick = onNavigateToProfile
                )
            }

            Divider()

            // AI Section
            SettingsSection(title = "AI & Intelligence") {
                SwitchSettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Anomaly Detection",
                    subtitle = "Detect unusual transactions",
                    checked = settings.aiAnomalyDetectionEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateAISettings(
                            enabled = enabled,
                            sensitivity = settings.aiSensitivity,
                            autoCorrect = settings.aiAutoCorrectEnabled,
                            suggestions = settings.aiSuggestionsEnabled
                        )
                    }
                )

                SettingsItem(
                    icon = Icons.Default.Tune,
                    title = "AI Settings",
                    subtitle = "Sensitivity: ${settings.aiSensitivity.name.lowercase().capitalize()}",
                    onClick = onNavigateToAI,
                    enabled = settings.aiAnomalyDetectionEnabled
                )

                SwitchSettingsItem(
                    icon = Icons.Default.Lightbulb,
                    title = "AI Suggestions",
                    subtitle = "Get smart recommendations",
                    checked = settings.aiSuggestionsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateAISettings(
                            enabled = settings.aiAnomalyDetectionEnabled,
                            sensitivity = settings.aiSensitivity,
                            autoCorrect = settings.aiAutoCorrectEnabled,
                            suggestions = enabled
                        )
                    },
                    enabled = settings.aiAnomalyDetectionEnabled
                )
            }

            Divider()

            // Appearance Section
            SettingsSection(title = "Appearance") {
                ThemeSelector(
                    currentTheme = settings.themeMode,
                    onThemeSelected = { theme ->
                        viewModel.updateTheme(theme, settings.useDynamicColors)
                    }
                )

                SwitchSettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Dynamic Colors",
                    subtitle = "Use system wallpaper colors",
                    checked = settings.useDynamicColors,
                    onCheckedChange = { enabled ->
                        viewModel.updateTheme(settings.themeMode, enabled)
                    }
                )
            }

            Divider()

            // Notifications Section
            SettingsSection(title = "Notifications") {
                SwitchSettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Quota Warnings",
                    subtitle = "Alert when approaching limit",
                    checked = settings.quotaWarningEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateNotifications(enabled, settings.invoiceRemindersEnabled)
                    }
                )

                SwitchSettingsItem(
                    icon = Icons.Default.Schedule,
                    title = "Invoice Reminders",
                    subtitle = "Remind about due invoices",
                    checked = settings.invoiceRemindersEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateNotifications(settings.quotaWarningEnabled, enabled)
                    }
                )
            }

            Divider()

            // Privacy Section
            SettingsSection(title = "Privacy & Data") {
                SwitchSettingsItem(
                    icon = Icons.Default.Analytics,
                    title = "Usage Analytics",
                    subtitle = "Help improve the app",
                    checked = settings.analyticsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updatePrivacy(enabled, settings.crashReportingEnabled)
                    }
                )

                SwitchSettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Crash Reporting",
                    subtitle = "Automatically report crashes",
                    checked = settings.crashReportingEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updatePrivacy(settings.analyticsEnabled, enabled)
                    }
                )
            }

            Divider()

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About INBusiness",
                    subtitle = "Version 3.0.0",
                    onClick = onNavigateToAbout
                )

                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = { /* TODO */ }
                )

                SettingsItem(
                    icon = Icons.Default.Gavel,
                    title = "Terms of Service",
                    subtitle = "Usage terms and conditions",
                    onClick = { /* TODO */ }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuotaCard(
    quotaInfo: QuotaInfo,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (quotaInfo.tier == "FREE")
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${quotaInfo.tier} Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (quotaInfo.tier == "FREE") {
                            "${quotaInfo.dailyUsed}/${quotaInfo.dailyLimit} invoices used today"
                        } else {
                            "Unlimited invoices"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (quotaInfo.tier == "FREE") {
                    FilledTonalButton(onClick = onUpgrade) {
                        Text("Upgrade")
                    }
                }
            }

            if (quotaInfo.bonusInvoices > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "🎁 +${quotaInfo.bonusInvoices} bonus invoices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, "Navigate")
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
private fun SwitchSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

@Composable
private fun ThemeSelector(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Theme") },
        supportingContent = { Text(currentTheme.name.lowercase().capitalize()) },
        leadingContent = {
            Icon(Icons.Default.DarkMode, "Theme")
        },
        trailingContent = {
            Icon(Icons.Default.ArrowDropDown, "Expand")
        },
        modifier = Modifier.clickable { expanded = true }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        ThemeMode.values().forEach { theme ->
            DropdownMenuItem(
                text = { Text(theme.name.lowercase().capitalize()) },
                onClick = {
                    onThemeSelected(theme)
                    expanded = false
                },
                leadingIcon = if (theme == currentTheme) {
                    { Icon(Icons.Default.Check, "Selected") }
                } else null
            )
        }
    }
}