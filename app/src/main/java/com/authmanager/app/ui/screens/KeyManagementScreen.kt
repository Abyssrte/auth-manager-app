package com.authmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.authmanager.app.data.AppViewModel
import com.authmanager.app.data.KeyRecord
import com.authmanager.app.ui.components.*
import com.authmanager.app.ui.theme.*
import com.authmanager.app.util.DurationUtil

private enum class KeyDialog { NONE, GENERATE, CUSTOM, CHANGE_LIMIT, DELETE }

@Composable
fun KeyManagementScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var dialog by remember { mutableStateOf(KeyDialog.NONE) }
    var selectedKey by remember { mutableStateOf<KeyRecord?>(null) }
    val nowMillis = remember { System.currentTimeMillis() }

    Column(modifier = Modifier.fillMaxSize().background(BgRoot)) {
        AppTopBar(title = "Key Management", onBack = onBack, onRefresh = { viewModel.refresh() })

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.banner?.let { banner ->
                item { Banner(banner.message, banner.isError) { viewModel.clearBanner() } }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(text = "➕ Generate", modifier = Modifier.weight(1f)) { dialog = KeyDialog.GENERATE }
                    SecondaryButton(text = "✏️ Custom", modifier = Modifier.weight(1f)) { dialog = KeyDialog.CUSTOM }
                }
            }

            item {
                Text(
                    "Keys (${state.snapshot?.keys?.size ?: 0})",
                    style = TitleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            if (state.isLoading && state.snapshot == null) {
                item { LoadingSpinner(modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            }

            val keys = state.snapshot?.keys.orEmpty()
            if (!state.isLoading && keys.isEmpty()) {
                item { Text("No keys yet. Generate one above.", style = BodyMedium, modifier = Modifier.padding(vertical = 16.dp)) }
            }

            items(keys) { record ->
                val deviceCount = state.snapshot?.devices?.count { it.key == record.key } ?: 0
                KeyCard(
                    record = record,
                    deviceCount = deviceCount,
                    nowMillis = nowMillis,
                    onChangeLimit = { selectedKey = record; dialog = KeyDialog.CHANGE_LIMIT },
                    onDelete = { selectedKey = record; dialog = KeyDialog.DELETE },
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    when (dialog) {
        KeyDialog.GENERATE -> GenerateKeyDialog(
            title = "Generate Key",
            onDismiss = { dialog = KeyDialog.NONE },
            onConfirm = { duration, limit ->
                viewModel.generateKey(duration, limit) { success, msg ->
                    dialog = KeyDialog.NONE
                }
            },
        )
        KeyDialog.CUSTOM -> CustomKeyDialog(
            onDismiss = { dialog = KeyDialog.NONE },
            onConfirm = { text, duration, limit ->
                viewModel.generateKey(duration, limit, text) { success, msg ->
                    dialog = KeyDialog.NONE
                }
            },
        )
        KeyDialog.CHANGE_LIMIT -> selectedKey?.let { key ->
            ChangeDurationDialog(
                keyText = key.key,
                onDismiss = { dialog = KeyDialog.NONE },
                onConfirm = { duration ->
                    viewModel.changeKeyDuration(key.key, duration) { success, msg ->
                        dialog = KeyDialog.NONE
                    }
                },
            )
        }
        KeyDialog.DELETE -> selectedKey?.let { key ->
            ConfirmDialog(
                title = "Delete key?",
                message = "This will permanently delete `${key.key}` and unregister all its devices.",
                confirmLabel = "Delete",
                isDestructive = true,
                onDismiss = { dialog = KeyDialog.NONE },
                onConfirm = {
                    viewModel.deleteKey(key.key) { success, msg ->
                        dialog = KeyDialog.NONE
                    }
                },
            )
        }
        KeyDialog.NONE -> {}
    }
}

@Composable
private fun KeyCard(
    record: KeyRecord,
    deviceCount: Int,
    nowMillis: Long,
    onChangeLimit: () -> Unit,
    onDelete: () -> Unit,
) {
    val label = DurationUtil.timeLeftLabel(record.expiry, nowMillis)
    val tone = when {
        label == "unlimited" -> BadgeTone.BLUE
        DurationUtil.isExpired(record.expiry, nowMillis) -> BadgeTone.RED
        DurationUtil.isUrgent(record.expiry, nowMillis) -> BadgeTone.YELLOW
        else -> BadgeTone.GREEN
    }
    val limitLabel = if (record.deviceLimit == 0) "∞" else record.deviceLimit.toString()

    SectionCard {
        CopyableMonoField(label = "KEY", value = record.key)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(text = label, tone = tone)
            StatusBadge(text = "Devices $deviceCount/$limitLabel", tone = BadgeTone.NEUTRAL)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Created: ${record.created}", style = LabelSmall, modifier = Modifier.padding(top = 6.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(text = "⏱️ Change Limit", modifier = Modifier.weight(1f), onClick = onChangeLimit)
            SecondaryButton(text = "🗑️ Delete", modifier = Modifier.weight(1f), isDestructive = true, onClick = onDelete)
        }
    }
}

@Composable
private fun GenerateKeyDialog(title: String, onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var duration by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text(title, style = TitleMedium) },
        text = {
            Column {
                DialogTextField(value = duration, onValueChange = { duration = it }, label = "Duration (30d, 5h, 10m, unlimited)")
                Spacer(modifier = Modifier.height(12.dp))
                DialogTextField(value = limit, onValueChange = { limit = it }, label = "Device limit", numeric = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val limitInt = limit.toIntOrNull() ?: 1
                if (duration.isNotBlank()) onConfirm(duration, limitInt)
            }) { Text("Generate", color = AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
    )
}

@Composable
private fun CustomKeyDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("Custom Key", style = TitleMedium) },
        text = {
            Column {
                DialogTextField(value = text, onValueChange = { text = it }, label = "Custom key text")
                Spacer(modifier = Modifier.height(12.dp))
                DialogTextField(value = duration, onValueChange = { duration = it }, label = "Duration (30d, 5h, 10m, unlimited)")
                Spacer(modifier = Modifier.height(12.dp))
                DialogTextField(value = limit, onValueChange = { limit = it }, label = "Device limit", numeric = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val limitInt = limit.toIntOrNull() ?: 1
                if (text.isNotBlank() && duration.isNotBlank()) onConfirm(text, duration, limitInt)
            }) { Text("Create", color = AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
    )
}

@Composable
private fun ChangeDurationDialog(keyText: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var duration by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("Change Duration", style = TitleMedium) },
        text = {
            Column {
                Text(keyText, style = MonospaceSmall, modifier = Modifier.padding(bottom = 12.dp))
                DialogTextField(value = duration, onValueChange = { duration = it }, label = "New duration (30d, 5h, 10m, unlimited)")
            }
        },
        confirmButton = {
            TextButton(onClick = { if (duration.isNotBlank()) onConfirm(duration) }) { Text("Update", color = AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text(title, style = TitleMedium) },
        text = { Text(message, style = BodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = if (isDestructive) StatusRed else AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
    )
}

@Composable
fun DialogTextField(value: String, onValueChange: (String) -> Unit, label: String, numeric: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = if (numeric) {
            androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        } else androidx.compose.foundation.text.KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = BorderSubtle,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = AccentBlue,
            unfocusedLabelColor = TextMuted,
        ),
    )
}
