package com.aktarjabed.inbusiness.presentation.screens.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onNavigateBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current as Activity

    var phoneNumber by remember { mutableStateOf("+91") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Verification") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Phone",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (verificationId == null) "Enter Phone Number" else "Enter OTP",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (verificationId == null)
                    "We'll send you a verification code"
                else
                    "Enter 6-digit code sent to $phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            if (verificationId == null) {
                // Phone Number Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        if (it.startsWith("+91") && it.length <= 13) {
                            phoneNumber = it
                        }
                    },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    supportingText = { Text("Format: +91XXXXXXXXXX") },
                    singleLine = true
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        isLoading = true
                        error = null
                        viewModel.sendOTP(
                            activity = context,
                            phoneNumber = phoneNumber,
                            onCodeSent = { id ->
                                verificationId = id
                                isLoading = false
                            },
                            onError = { e ->
                                error = e.message
                                isLoading = false
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phoneNumber.length == 13 && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Send OTP")
                    }
                }
            } else {
                // OTP Input
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            otpCode = it
                        }
                    },
                    label = { Text("OTP Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    supportingText = { Text("6-digit code") },
                    singleLine = true
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        isLoading = true
                        error = null
                        viewModel.verifyOTP(
                            verificationId = verificationId!!,
                            code = otpCode,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onError = { e ->
                                error = e.message
                                isLoading = false
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = otpCode.length == 6 && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Verify OTP")
                    }
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        verificationId = null
                        otpCode = ""
                        error = null
                    },
                    enabled = !isLoading
                ) {
                    Text("Change Phone Number")
                }
            }

            error?.let { errorMessage ->
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}