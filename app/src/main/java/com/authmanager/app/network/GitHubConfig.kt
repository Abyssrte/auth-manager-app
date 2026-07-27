package com.authmanager.app.network

/**
 * Repo configuration. The GitHub token itself is NOT here — it's loaded at
 * runtime from a hidden file on-device via SecretConfig, so it never touches
 * git and never trips GitHub's push protection.
 */
object GitHubConfig {
    const val OWNER = "Abyssrte"
    const val REPO = "authbot"
    const val BRANCH = "main"

    const val KEYS_PATH = "keys.txt"
    const val DEVICES_PATH = "devices.txt"
    const val BLOCKED_PATH = "blocked.txt"

    // App login credentials — not device-bound, just a simple username/password gate.
    const val LOGIN_USERNAME = "Abyssrte"
    const val LOGIN_PASSWORD = "admin"
}
