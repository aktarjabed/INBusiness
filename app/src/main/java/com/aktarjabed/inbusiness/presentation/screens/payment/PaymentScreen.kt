package com.aktarjabed.inbusiness.presentation.screens.payment

import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
import com.aktarjabed.inbusiness.domain.model.SubscriptionPlan
import com.aktarjabed.inbusiness.presentation.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onPaymentSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current as Activity
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.showSuccessDialog) {
        if (uiState.showSuccessDialog) {
            onPaymentSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Premium") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Unlock premium features for your business",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            PlanCard(
                plan = SubscriptionPlan.BASIC,
                isSelected = uiState.selectedPlan == SubscriptionPlan.BASIC,
                onSelect = { viewModel.selectPlan(SubscriptionPlan.BASIC) }
            )

            Spacer(Modifier.height(16.dp))

            PlanCard(
                plan = SubscriptionPlan.PRO,
                isSelected = uiState.selectedPlan == SubscriptionPlan.PRO,
                onSelect = { viewModel.selectPlan(SubscriptionPlan.PRO) },
                recommended = true
            )

            Spacer(Modifier.height(16.dp))

            PlanCard(
                plan = SubscriptionPlan.ENTERPRISE,
                isSelected = uiState.selectedPlan == SubscriptionPlan.ENTERPRISE,
                onSelect = { viewModel.selectPlan(SubscriptionPlan.ENTERPRISE) }
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "All Indian Payment Methods Supported",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    PaymentMethodRow(Icons.Default.AccountBalance, "UPI (GPay, PhonePe, Paytm, BHIM)")
                    PaymentMethodRow(Icons.Default.CreditCard, "Cards (Visa, Master, RuPay)")
                    PaymentMethodRow(Icons.Default.AccountBalance, "Net Banking (50+ banks)")
                    PaymentMethodRow(Icons.Default.Wallet, "Wallets (Paytm, Amazon Pay)")
                    PaymentMethodRow(Icons.Default.Money, "EMI & Pay Later Options")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    uiState.selectedPlan?.let { plan ->
                        viewModel.initiatePayment(context, plan)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.selectedPlan != null && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, "Secure", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pay ₹${uiState.selectedPlan?.priceINR ?: 0}/month")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "🔒 Secure payment powered by Razorpay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            uiState.error?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    icon = { Icon(Icons.Default.Error, "Error") },
                    title = { Text("Payment Failed") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onSelect: () -> Unit,
    recommended: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (recommended) {
                        Spacer(Modifier.height(4.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("Recommended") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${plan.priceINR}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "per month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            plan.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}