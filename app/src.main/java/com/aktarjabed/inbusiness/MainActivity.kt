package com.aktarjabed.inbusiness

import android.os.Bundle
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
        setContent {
            InBusinessTheme {
                Surface {
                    InBusinessNavGraph()
                }
            }
        }
    }
}