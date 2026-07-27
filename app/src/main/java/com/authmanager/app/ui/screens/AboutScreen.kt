package com.authmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.authmanager.app.AppInfo
import com.authmanager.app.ui.components.AppTopBar
import com.authmanager.app.ui.components.SectionCard
import com.authmanager.app.ui.theme.*

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BgRoot)) {
        AppTopBar(title = "About", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("🔐", style = TitleLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Auth Manager", style = TitleLarge)
            Text("Version ${AppInfo.VERSION_NAME}", style = BodyMedium, modifier = Modifier.padding(top = 2.dp))

            Spacer(modifier = Modifier.height(24.dp))

            SectionCard {
                InfoRow(label = "Developer", value = AppInfo.DEVELOPER_NAME)
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(label = "Telegram", value = AppInfo.TELEGRAM_USERNAME)
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(label = "GitHub", value = AppInfo.GITHUB_USERNAME)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = BodyMedium)
        Text(value, style = TitleMedium.copy(color = AccentBlue))
    }
}
