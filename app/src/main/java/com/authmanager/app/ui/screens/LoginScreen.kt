package com.authmanager.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.authmanager.app.data.SessionStore
import com.authmanager.app.network.GitHubConfig
import com.authmanager.app.ui.components.PrimaryButton
import com.authmanager.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showSuccessPopup by remember { mutableStateOf(false) }
    var checkingSession by remember { mutableStateOf(true) }

    // On first composition, silently auto-login if a previous session chose "Remember Me".
    LaunchedEffect(Unit) {
        if (sessionStore.isRemembered()) {
            onLoginSuccess()
        } else {
            checkingSession = false
        }
    }

    fun attemptLogin() {
        val isValid = username == GitHubConfig.LOGIN_USERNAME && password == GitHubConfig.LOGIN_PASSWORD
        if (isValid) {
            scope.launch { sessionStore.setRememberMe(rememberMe) }
            showSuccessPopup = true
        } else {
            showError = true
        }
    }

    if (checkingSession) return

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
                value = username,
                onValueChange = {
                    username = it
                    showError = false
                },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = showError,
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    showError = false
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = showError,
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rememberMe = !rememberMe },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentBlue,
                        uncheckedColor = TextMuted,
                        checkmarkColor = BgRoot,
                    ),
                )
                Text("Remember me", style = BodyMedium)
            }

            // Deliberately generic — never reveals whether the username or the
            // password was the incorrect part.
            AnimatedVisibility(visible = showError) {
                Text(
                    "Invalid username or password",
                    style = LabelSmall.copy(color = StatusRed),
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(text = "Login") { attemptLogin() }
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
