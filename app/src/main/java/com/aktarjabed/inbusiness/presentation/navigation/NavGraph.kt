package com.aktarjabed.inbusiness.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aktarjabed.inbusiness.presentation.screens.CalculatorScreen
import com.aktarjabed.inbusiness.presentation.screens.DashboardScreen

@Composable
fun InBusinessNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen { navController.navigate("calculator") }
        }
        composable("calculator") {
            CalculatorScreen()
        }
    }
}