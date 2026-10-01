package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.invoice.GstCalculator
import com.aktarjabed.inbusiness.presentation.viewmodel.SetupViewModel

@Composable
fun SetupScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }

    val setupComplete by viewModel.setupComplete.collectAsState()
    val error by viewModel.error.collectAsState()

    // Surface an invalid GSTIN immediately instead of letting it silently disable
    // supply-type detection (and therefore invoicing) later on.
    val gstinInvalid = gstin.isNotBlank() && !GstCalculator.isValidGstin(gstin)

    LaunchedEffect(setupComplete) {
        if (setupComplete) {
            onSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Business Setup", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Business Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Business Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = gstin,
            onValueChange = { gstin = it.uppercase() },
            label = { Text("GSTIN (Optional)") },
            isError = gstinInvalid,
            supportingText = {
                if (gstinInvalid) {
                    Text("Enter a valid 15-character GSTIN or leave this blank")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.setupBusiness(name, address, gstin) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && address.isNotBlank() && !gstinInvalid
        ) {
            Text("Complete Setup")
        }
    }
}
