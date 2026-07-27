package com.authmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.authmanager.app.network.GitHubConfig
import com.authmanager.app.ui.components.PrimaryButton
import com.authmanager.app.ui.theme.*
import com.authmanager.app.util.DeviceHash

private enum class LoginError { NONE, WRONG_PASSWORD, WRONG_DEVICE }

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(LoginError.NONE) }
    var showSuccessPopup by remember { mutableStateOf(false) }

    // This device's own hash prefix — computed locally, never sent anywhere.
    // Shown read-only as the "username" so the admin can see what this device
    // identifies as, and compared below against the hash baked into this build.
    val deviceHashPrefix = remember { DeviceHash.computePrefix() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgRoot)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔐", style = TitleLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Auth Manager", style = TitleLarge)
            Text("Sign in to continue", style = BodyMedium, modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = deviceHashPrefix,
                onValueChange = {},
                readOnly = true,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = BorderSubtle,
                    focusedLabelColor = AccentBlue,
                    unfocusedLabelColor = TextMuted,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    loginError = LoginError.NONE
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = loginError != LoginError.NONE,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = AccentBlue,
                    unfocusedLabelColor = TextMuted,
                    errorBorderColor = StatusRed,
                ),
            )

            AnimatedVisibility(visible = loginError != LoginError.NONE) {
                val message = when (loginError) {
                    LoginError.WRONG_PASSWORD -> "Incorrect password"
                    LoginError.WRONG_DEVICE -> "This device isn't authorized for this app"
                    LoginError.NONE -> ""
                }
                Text(
                    message,
                    style = LabelSmall.copy(color = StatusRed),
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(text = "Login") {
                loginError = when {
                    deviceHashPrefix != GitHubConfig.ADMIN_HASH_PREFIX -> LoginError.WRONG_DEVICE
                    password != GitHubConfig.LOGIN_PASSWORD -> LoginError.WRONG_PASSWORD
                    else -> {
                        showSuccessPopup = true
                        LoginError.NONE
                    }
                }
            }
        }

        if (showSuccessPopup) {
            LoginSuccessPopup(onDismiss = {
                showSuccessPopup = false
                onLoginSuccess()
            })
        }
    }
}

@Composable
private fun LoginSuccessPopup(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(700)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✅", style = TitleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Login success", style = TitleMedium.copy(color = StatusGreen))
        }
    }
}
