package com.authmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.authmanager.app.data.AppViewModel
import com.authmanager.app.ui.components.Banner
import com.authmanager.app.ui.components.MenuRow
import com.authmanager.app.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenKeyManagement: () -> Unit,
    onOpenDeviceManagement: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val keyCount = state.snapshot?.keys?.size ?: 0
    val deviceCount = state.snapshot?.devices?.size ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgRoot)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Auth Manager", style = TitleLarge)
            Text("Manage keys and devices", style = BodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        }

        state.banner?.let { banner ->
            item {
                Banner(message = banner.message, isError = banner.isError, onDismiss = { viewModel.clearBanner() })
            }
        }

        item {
            MenuRow(
                icon = "🔑",
                title = "Key Management",
                subtitle = "$keyCount key(s) — generate, edit, delete",
                onClick = onOpenKeyManagement,
            )
        }
        item {
            MenuRow(
                icon = "💻",
                title = "Device Management",
                subtitle = "$deviceCount device(s) — register, block",
                onClick = onOpenDeviceManagement,
            )
        }
    }
}
