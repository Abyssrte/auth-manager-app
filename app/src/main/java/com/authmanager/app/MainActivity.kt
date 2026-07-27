package com.authmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.authmanager.app.ui.AppNavHost
import com.authmanager.app.ui.theme.AuthManagerTheme
import com.authmanager.app.ui.theme.BgRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuthManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(BgRoot),
                    color = BgRoot,
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
