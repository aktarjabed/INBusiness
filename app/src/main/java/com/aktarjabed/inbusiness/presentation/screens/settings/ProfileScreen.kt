package com.aktarjabed.inbusiness.presentation.screens.settings

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.userSettings.collectAsState()

    var userName by remember { mutableStateOf(settings.userName) }
    var userEmail by remember { mutableStateOf(settings.userEmail) }
    var businessName by remember { mutableStateOf(settings.businessName) }
    var gstin by remember { mutableStateOf(settings.gstin) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    // Update local state when settings change
    LaunchedEffect(settings) {
        userName = settings.userName
        userEmail = settings.userEmail
        businessName = settings.businessName
        gstin = settings.gstin
    }

    // Check for changes
    LaunchedEffect(userName, userEmail, businessName, gstin) {
        hasChanges = userName != settings.userName ||
                userEmail != settings.userEmail ||
                businessName != settings.businessName ||
                gstin != settings.gstin
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (hasChanges) {
                        TextButton(
                            onClick = {
                                viewModel.updateUserProfile(
                                    userName,
                                    userEmail,
                                    businessName,
                                    gstin
                                )
                                showSaveDialog = true
                            }
                        ) {
                            Text("Save")
                        }
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "This information will be used in invoices and receipts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // User Name
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Your Name") },
                leadingIcon = {
                    Icon(Icons.Default.Person, "Name")
                },
                supportingText = {
                    Text("This appears on invoices as the seller")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Email
            OutlinedTextField(
                value = userEmail,
                onValueChange = { userEmail = it },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(Icons.Default.Email, "Email")
                },
                supportingText = {
                    Text("For notifications and account recovery")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            // Business Information
            Text(
                text = "Business Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Business Name
            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business Name") },
                leadingIcon = {
                    Icon(Icons.Default.Business, "Business")
                },
                supportingText = {
                    Text("Legal name of your business")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // GSTIN
            OutlinedTextField(
                value = gstin,
                onValueChange = {
                    // Auto-format GSTIN (15 characters, uppercase)
                    gstin = it.uppercase().take(15)
                },
                label = { Text("GSTIN") },
                leadingIcon = {
                    Icon(Icons.Default.Numbers, "GSTIN")
                },
                supportingText = {
                    Text("15-digit GST Identification Number")
                },
                isError = gstin.isNotEmpty() && gstin.length != 15,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (gstin.isNotEmpty() && gstin.length == 15) {
                GSTINInfo(gstin)
            }

            Spacer(Modifier.height(16.dp))

            // Auto-detect button
            OutlinedButton(
                onClick = { /* TODO: Implement auto-detect from last invoice */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, "Auto-detect", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Auto-fill from last invoice")
            }

            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        "Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Privacy Notice",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your information is stored locally on your device with bank-grade encryption. We never share your data with third parties.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Save confirmation dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, "Success", tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Profile Updated") },
            text = { Text("Your profile information has been saved successfully.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun GSTINInfo(gstin: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "GSTIN Details",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Extract state code (first 2 digits)
            val stateCode = gstin.take(2)
            val panNumber = gstin.substring(2, 12)
            val entityNumber = gstin.substring(12, 13)
            val checksum = gstin.last()

            GSTINDetailRow("State Code", stateCode, getStateName(stateCode))
            GSTINDetailRow("PAN Number", panNumber, "Business PAN")
            GSTINDetailRow("Entity Number", entityNumber, "Registration sequence")
            GSTINDetailRow("Checksum", checksum.toString(), "Verification digit")
        }
    }
}

@Composable
private fun GSTINDetailRow(label: String, value: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getStateName(code: String): String {
    return when (code) {
        "01" -> "Jammu & Kashmir"
        "02" -> "Himachal Pradesh"
        "03" -> "Punjab"
        "04" -> "Chandigarh"
        "05" -> "Uttarakhand"
        "06" -> "Haryana"
        "07" -> "Delhi"
        "08" -> "Rajasthan"
        "09" -> "Uttar Pradesh"
        "10" -> "Bihar"
        "11" -> "Sikkim"
        "12" -> "Arunachal Pradesh"
        "13" -> "Nagaland"
        "14" -> "Manipur"
        "15" -> "Mizoram"
        "16" -> "Tripura"
        "17" -> "Meghalaya"
        "18" -> "Assam"
        "19" -> "West Bengal"
        "20" -> "Jharkhand"
        "21" -> "Odisha"
        "22" -> "Chhattisgarh"
        "23" -> "Madhya Pradesh"
        "24" -> "Gujarat"
        "27" -> "Maharashtra"
        "29" -> "Karnataka"
        "30" -> "Goa"
        "32" -> "Kerala"
        "33" -> "Tamil Nadu"
        "34" -> "Puducherry"
        "35" -> "Andaman & Nicobar"
        "36" -> "Telangana"
        "37" -> "Andhra Pradesh"
        else -> "Unknown State"
    }
}