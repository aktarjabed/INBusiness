package com.aktarjabed.inbusiness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.aktarjabed.inbusiness.data.repository.AuthRepository
import com.aktarjabed.inbusiness.data.repository.PaymentRepository
import com.aktarjabed.inbusiness.presentation.navigation.AppNavigation
import com.aktarjabed.inbusiness.presentation.theme.INBusinessTheme
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultListener {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            INBusinessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by authRepository.currentUserFlow.collectAsState(initial = null)

                    AppNavigation(
                        isAuthenticated = currentUser != null,
                        startDestination = if (currentUser != null) "dashboard" else "login"
                    )
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Timber.i("Payment success: $razorpayPaymentId")
        razorpayPaymentId?.let {
            paymentRepository.onPaymentSuccess(it)
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        Timber.e("Payment error $code: $response")
        paymentRepository.onPaymentError(code, response)
    }
}