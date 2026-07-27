package com.authmanager.app

import android.os.Bundle
import android.view.WindowManager
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
        // Explicitly ensure screenshots/screen-recording aren't blocked — this app
        // never sets FLAG_SECURE, but some device skins (e.g. MIUI) can restrict
        // capture on apps they judge "sensitive". Clearing it here is a no-op if
        // it was never set, and removes it if the OS added it on its own.
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

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
