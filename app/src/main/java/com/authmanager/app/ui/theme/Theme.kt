package com.authmanager.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = BgRoot,
    secondary = StatusGreen,
    background = BgRoot,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextSecondary,
    error = StatusRed,
    onError = TextPrimary,
    outline = BorderSubtle,
)

@Composable
fun AuthManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // App is dark-only by design — the admin UI always uses the dark palette
    // regardless of system theme, for a consistent professional look.
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content,
    )
}
