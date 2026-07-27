package com.authmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.authmanager.app.data.AppViewModel
import com.authmanager.app.data.DeviceRecord
import com.authmanager.app.ui.components.*
import com.authmanager.app.ui.theme.*

private enum class DeviceDialog { NONE, REGISTER, UNREGISTER, BLOCK, UNBLOCK }
private enum class DeviceTab { REGISTRATION, BLOCKED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var dialog by remember { mutableStateOf(DeviceDialog.NONE) }
    var selectedHash by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(DeviceTab.REGISTRATION) }

    Column(modifier = Modifier.fillMaxSize().background(BgRoot)) {
        AppTopBar(title = "Device Management", onBack = onBack, onRefresh = { viewModel.refresh() })

        TabRow(
            selectedTabIndex = if (tab == DeviceTab.REGISTRATION) 0 else 1,
            containerColor = BgRoot,
            contentColor = AccentBlue,
        ) {
            Tab(
                selected = tab == DeviceTab.REGISTRATION,
                onClick = { tab = DeviceTab.REGISTRATION },
                text = { Text("Registration") },
            )
            Tab(
                selected = tab == DeviceTab.BLOCKED,
                onClick = { tab = DeviceTab.BLOCKED },
                text = { Text("Blocked") },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }

            state.banner?.let { banner ->
                item { Banner(banner.message, banner.isError) { viewModel.clearBanner() } }
            }

            if (tab == DeviceTab.REGISTRATION) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(text = "📱 Register", modifier = Modifier.weight(1f)) { dialog = DeviceDialog.REGISTER }
                        SecondaryButton(text = "📤 Unregister", modifier = Modifier.weight(1f)) { dialog = DeviceDialog.UNREGISTER }
                    }
                }

                val devices = state.snapshot?.devices.orEmpty()
                val blockedHashes = state.snapshot?.blocked?.map { it.hash }.orEmpty().toSet()

                item {
                    Text("Registered (${devices.size})", style = TitleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                if (!state.isLoading && devices.isEmpty()) {
                    item { Text("No devices registered yet.", style = BodyMedium, modifier = Modifier.padding(vertical = 16.dp)) }
                }
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        isBlocked = device.hash in blockedHashes,
                        onUnregister = { selectedHash = device.hash; dialog = DeviceDialog.UNREGISTER },
                    )
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(text = "🚫 Block", modifier = Modifier.weight(1f)) { dialog = DeviceDialog.BLOCK }
                        SecondaryButton(text = "✅ Unblock", modifier = Modifier.weight(1f)) { dialog = DeviceDialog.UNBLOCK }
                    }
                }

                val blocked = state.snapshot?.blocked.orEmpty()
                item {
                    Text("Blocked (${blocked.size})", style = TitleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                if (!state.isLoading && blocked.isEmpty()) {
                    item { Text("No blocked devices.", style = BodyMedium, modifier = Modifier.padding(vertical = 16.dp)) }
                }
                items(blocked) { entry ->
                    SectionCard {
                        CopyableMonoField(label = "DEVICE HASH", value = entry.hash)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Blocked: ${entry.blockedAt}", style = LabelSmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryButton(text = "✅ Unblock", onClick = {
                            selectedHash = entry.hash
                            dialog = DeviceDialog.UNBLOCK
                        })
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    when (dialog) {
        DeviceDialog.REGISTER -> RegisterDeviceDialog(
            keys = state.snapshot?.keys.orEmpty().map { it.key },
            onDismiss = { dialog = DeviceDialog.NONE },
            onConfirm = { key, hash ->
                viewModel.registerDevice(key, hash) { _, _ -> dialog = DeviceDialog.NONE }
            },
        )
        DeviceDialog.UNREGISTER -> HashInputDialog(
            title = "Unregister Device",
            prefill = selectedHash.orEmpty(),
            confirmLabel = "Unregister",
            onDismiss = { dialog = DeviceDialog.NONE; selectedHash = null },
            onConfirm = { hash ->
                viewModel.unregisterDevice(hash) { _, _ -> dialog = DeviceDialog.NONE; selectedHash = null }
            },
        )
        DeviceDialog.BLOCK -> HashInputDialog(
            title = "Block Device",
            prefill = "",
            confirmLabel = "Block",
            isDestructive = true,
            onDismiss = { dialog = DeviceDialog.NONE },
            onConfirm = { hash ->
                viewModel.blockDevice(hash) { _, _ -> dialog = DeviceDialog.NONE }
            },
        )
        DeviceDialog.UNBLOCK -> HashInputDialog(
            title = "Unblock Device",
            prefill = selectedHash.orEmpty(),
            confirmLabel = "Unblock",
            onDismiss = { dialog = DeviceDialog.NONE; selectedHash = null },
            onConfirm = { hash ->
                viewModel.unblockDevice(hash) { _, _ -> dialog = DeviceDialog.NONE; selectedHash = null }
            },
        )
        DeviceDialog.NONE -> {}
    }
}

@Composable
private fun DeviceCard(device: DeviceRecord, isBlocked: Boolean, onUnregister: () -> Unit) {
    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(text = "Key: ${device.key}", tone = BadgeTone.BLUE)
            if (isBlocked) StatusBadge(text = "BLOCKED", tone = BadgeTone.RED)
        }
        Spacer(modifier = Modifier.height(8.dp))
        CopyableMonoField(label = "DEVICE HASH", value = device.hash)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Registered: ${device.registered}", style = LabelSmall)
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(text = "📤 Unregister", isDestructive = true, onClick = onUnregister)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterDeviceDialog(keys: List<String>, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var selectedKey by remember { mutableStateOf(keys.firstOrNull().orEmpty()) }
    var hash by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("Register Device", style = TitleMedium) },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedKey,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Key") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentBlue,
                            unfocusedLabelColor = TextMuted,
                        ),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (keys.isEmpty()) {
                            DropdownMenuItem(text = { Text("No keys available") }, onClick = {})
                        }
                        keys.forEach { k ->
                            DropdownMenuItem(text = { Text(k) }, onClick = { selectedKey = k; expanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                DialogTextField(value = hash, onValueChange = { hash = it }, label = "Device hash")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedKey.isNotBlank() && hash.isNotBlank()) onConfirm(selectedKey, hash)
            }) { Text("Register", color = AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
    )
}

@Composable
private fun HashInputDialog(
    title: String,
    prefill: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var hash by remember { mutableStateOf(prefill) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text(title, style = TitleMedium) },
        text = { DialogTextField(value = hash, onValueChange = { hash = it }, label = "Device hash") },
        confirmButton = {
            TextButton(onClick = { if (hash.isNotBlank()) onConfirm(hash) }) {
                Text(confirmLabel, color = if (isDestructive) StatusRed else AccentBlue)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
    )
}
