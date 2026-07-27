package com.authmanager.app.network

import android.os.Environment
import java.io.File

/**
 * Loads the GitHub token (and any other secrets) from a hidden file on-device at
 * runtime, instead of having it compiled into the APK. This means:
 *   - the token never touches git, never gets committed, never trips GitHub's
 *     push protection, and GitHub Actions can build the APK with zero secrets
 *   - the token only exists on the one phone where you place the file
 *
 * Expected file: /storage/emulated/0/Download/.private_token
 * Format: plain text, one "KEY=value" pair per line, e.g.:
 *   TOKEN=github_pat_xxxxxxxxxxxx
 *
 * If the file is missing or malformed, every screen that needs GitHub access
 * shows a clear "Token not configured" message — the app never crashes for this.
 */
object SecretConfig {

    private const val FILE_NAME = ".private_token"

    private var cachedToken: String? = null
    private var lastLoadAttemptFailed = false

    /** Returns the token, or null if the file is missing/unreadable/malformed. */
    fun getToken(): String? {
        cachedToken?.let { return it }

        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
        if (!file.exists() || !file.canRead()) {
            lastLoadAttemptFailed = true
            return null
        }

        return try {
            val token = file.readLines()
                .map { it.trim() }
                .firstOrNull { it.startsWith("TOKEN=") }
                ?.substringAfter("TOKEN=")
                ?.trim()

            if (token.isNullOrBlank()) {
                lastLoadAttemptFailed = true
                null
            } else {
                cachedToken = token
                lastLoadAttemptFailed = false
                token
            }
        } catch (e: Exception) {
            lastLoadAttemptFailed = true
            null
        }
    }

    fun isConfigured(): Boolean = getToken() != null

    /** Human-readable reason to show in the UI when the token can't be loaded. */
    fun missingTokenMessage(): String =
        "GitHub token not found.\nCreate /storage/emulated/0/Download/.private_token with a line:\nTOKEN=your_github_token_here"

    /** Call after writing/updating the file at runtime (rare) to force a re-read. */
    fun invalidateCache() {
        cachedToken = null
    }
}
