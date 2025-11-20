package com.aktarjabed.inbusiness.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aktarjabed.inbusiness.presentation.screens.CalculatorScreen
import com.aktarjabed.inbusiness.presentation.screens.DashboardScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice.CreateInvoiceScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice.InvoiceListScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice.PdfPreviewScreen

@Composable
fun InBusinessNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(onNavigateToCalculator = { navController.navigate("calculator") },
                onNavigateToInvoices = { navController.navigate("invoice_list") })
        }
        composable("calculator") {
            CalculatorScreen()
        }
        composable("invoice_list") {
            InvoiceListScreen(
                onNewInvoice = { navController.navigate("create_invoice") },
                onEditInvoice = { id -> navController.navigate("create_invoice?invoiceId=$id") },
                onPreviewPdf = { id -> navController.navigate("pdf_preview/$id") }
            )
        }
        composable(
            route = "create_invoice?invoiceId={invoiceId}",
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType; defaultValue = "" })
        ) { backStack ->
            val invoiceId = backStack.arguments?.getString("invoiceId").orEmpty()
            CreateInvoiceScreen(
                invoiceId = invoiceId,
                onBack = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = "pdf_preview/{invoiceId}",
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) { backStack ->
            val invoiceId = backStack.arguments?.getString("invoiceId").orEmpty()
            PdfPreviewScreen(
                invoiceId = invoiceId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}