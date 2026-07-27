package com.authmanager.app.data

/**
 * Mirrors one line of keys.txt:
 *   key|expiry|deviceLimit|created
 * expiry is either "unlimited" or a timestamp "YYYY-MM-DD HH:MM:SS".
 */
data class KeyRecord(
    val key: String,
    val expiry: String,
    val deviceLimit: Int,
    val created: String,
) {
    fun toLine(): String = "$key|$expiry|$deviceLimit|$created"

    companion object {
        fun fromLine(line: String): KeyRecord? {
            val parts = line.split("|")
            if (parts.size < 4) return null
            val limit = parts[2].toIntOrNull() ?: return null
            return KeyRecord(parts[0], parts[1], limit, parts[3])
        }
    }
}

/**
 * Mirrors one line of devices.txt:
 *   key|deviceHash|model|brand|board|registeredDate
 */
data class DeviceRecord(
    val key: String,
    val hash: String,
    val model: String,
    val brand: String,
    val board: String,
    val registered: String,
) {
    fun toLine(): String = "$key|$hash|$model|$brand|$board|$registered"

    companion object {
        fun fromLine(line: String): DeviceRecord? {
            val parts = line.split("|")
            if (parts.size < 6) return null
            return DeviceRecord(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
        }
    }
}

/**
 * Mirrors one line of blocked.txt:
 *   deviceHash|blockedDate
 */
data class BlockedRecord(
    val hash: String,
    val blockedAt: String,
) {
    fun toLine(): String = "$hash|$blockedAt"

    companion object {
        fun fromLine(line: String): BlockedRecord? {
            val parts = line.split("|")
            if (parts.size < 2) return null
            return BlockedRecord(parts[0], parts[1])
        }
    }
}

/** Aggregate snapshot of everything fetched from the repo, plus where it came from. */
data class RepoSnapshot(
    val keys: List<KeyRecord>,
    val devices: List<DeviceRecord>,
    val blocked: List<BlockedRecord>,
    val fromCache: Boolean,
    val fetchedAt: Long,
)
