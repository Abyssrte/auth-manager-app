package com.authmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    onOpenAbout: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val keyCount = state.snapshot?.keys?.size ?: 0
    val deviceCount = state.snapshot?.devices?.size ?: 0
    var menuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgRoot)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text("Auth Manager", style = TitleLarge)
                    Text("Manage keys and devices", style = BodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                }
                Box {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier
                            .clickable { menuExpanded = true }
                            .padding(4.dp),
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = { menuExpanded = false; onOpenAbout() },
                        )
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = { menuExpanded = false; viewModel.logout(); onLoggedOut() },
                        )
                    }
                }
            }
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
