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

    // First 10 characters of the admin device hash — shown as the login "username"
    // and checked against what the device itself computes at login time.
    const val ADMIN_HASH_PREFIX = "d1421cf2c8"

    const val LOGIN_PASSWORD = "admin"
}
