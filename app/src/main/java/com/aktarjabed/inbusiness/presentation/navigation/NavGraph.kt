package com.aktarjabed.inbusiness.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aktarjabed.inbusiness.presentation.screens.CalculatorScreen
import com.aktarjabed.inbusiness.presentation.screens.DashboardScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice.InvoiceScreen
import com.aktarjabed.inbusiness.presentation.screens.settings.AISettingsScreen
import com.aktarjabed.inbusiness.presentation.screens.settings.AboutScreen
import com.aktarjabed.inbusiness.presentation.screens.settings.ProfileScreen
import com.aktarjabed.inbusiness.presentation.screens.settings.SettingsScreen

@Composable
fun InBusinessNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToCalculator = { navController.navigate("calculator") },
                onNavigateToInvoice = { navController.navigate("invoice") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("calculator") {
            CalculatorScreen()
        }
        composable("invoice") {
            InvoiceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpgrade = { /* TODO: Navigate to upgrade screen */ }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToAI = { navController.navigate("ai_settings") },
                onNavigateToAbout = { navController.navigate("about") },
                onNavigateToUpgrade = { /* TODO: Navigate to upgrade screen */ },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("ai_settings") {
            AISettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("profile") {
            ProfileScreen()
        }
        composable("about") {
            AboutScreen()
        }
    }
}