package com.aktarjabed.inbusiness

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aktarjabed.inbusiness.presentation.navigation.InBusinessNavGraph
import com.aktarjabed.inbusiness.presentation.theme.InBusinessTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Every destination in this activity can display financial or identifying data.
        // Prevent OS screenshots, screen recordings, and non-secure display mirroring by default.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            InBusinessTheme {
                Surface {
                    InBusinessNavGraph()
                }
            }
        }
    }
}