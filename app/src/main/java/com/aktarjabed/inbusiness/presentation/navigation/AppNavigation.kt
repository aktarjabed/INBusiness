package com.aktarjabed.inbusiness.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aktarjabed.inbusiness.presentation.screens.auth.LoginScreen
import com.aktarjabed.inbusiness.presentation.screens.auth.PhoneAuthScreen
import com.aktarjabed.inbusiness.presentation.screens.payment.PaymentScreen

@Composable
fun AppNavigation(
    isAuthenticated: Boolean,
    startDestination: String
) {
    val navController = rememberNavController()

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated && startDestination != "login") {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToPhoneAuth = {
                    navController.navigate("phone_auth")
                },
                onNavigateToSignUp = {
                    // TODO: Implement sign up screen
                }
            )
        }

        composable("phone_auth") {
            PhoneAuthScreen(
                onNavigateBack = { navController.popBackStack() },
                onAuthSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardPlaceholder(
                onNavigateToPayment = {
                    navController.navigate("payment")
                }
            )
        }

        composable("payment") {
            PaymentScreen(
                onPaymentSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("payment") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun DashboardPlaceholder(onNavigateToPayment: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateToPayment) {
            Text("Upgrade to Premium")
        }
    }
}