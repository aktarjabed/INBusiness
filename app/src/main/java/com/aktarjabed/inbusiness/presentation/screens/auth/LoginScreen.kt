package com.aktarjabed.inbusiness.presentation.screens.auth

import android.app.Activity
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.R
import com.aktarjabed.inbusiness.presentation.viewmodel.AuthViewModel
import com.aktarjabed.inbusiness.presentation.viewmodel.AuthUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToPhoneAuth: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current as Activity
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onLoginSuccess()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Welcome to INBusiness",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Google Sign-In
            OutlinedButton(
                onClick = { viewModel.signInWithGoogle(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is AuthUiState.Loading
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google")
            }

            Spacer(Modifier.height(12.dp))

            // Phone Sign-In
            OutlinedButton(
                onClick = onNavigateToPhoneAuth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is AuthUiState.Loading
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Continue with Phone (OTP)")
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading,
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            "Toggle password"
                        )
                    }
                },
                visualTransformation = if (showPassword)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading,
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signInWithEmail(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() &&
                         password.isNotBlank() &&
                         uiState !is AuthUiState.Loading
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign In")
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onNavigateToSignUp,
                enabled = uiState !is AuthUiState.Loading
            ) {
                Text("Don't have an account? Sign Up")
            }

            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "By signing in, you agree to our Terms & Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}