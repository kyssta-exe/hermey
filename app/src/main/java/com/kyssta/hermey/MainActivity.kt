package com.kyssta.hermey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kyssta.hermey.navigation.AppNavigation
import com.kyssta.hermey.ui.theme.HermexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HermexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.kyssta.hermey.ui.HermesColors.Background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
