package com.authmanager.app.data

import android.content.Context
import com.authmanager.app.network.GitHubClient
import com.authmanager.app.network.GitHubConfig
import com.authmanager.app.network.GitHubException
import com.authmanager.app.util.DurationUtil
import com.authmanager.app.util.RealTimeProvider
import java.util.UUID
import kotlin.random.Random

sealed class RepoResult<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean) : RepoResult<T>()
    data class Failure(val message: String) : RepoResult<Nothing>()
}

/**
 * Single source of truth for keys/devices/blocked data. Every screen goes through
 * here — it always tries a live GitHub fetch first, falls back to the local cache
 * on failure, and re-syncs the cache automatically whenever a live fetch succeeds.
 */
class AuthRepository(context: Context) {

    private val cache = LocalCache(context.applicationContext)

    // Remembered SHAs from the last successful fetch — needed to write back without
    // triggering a "stale write" conflict from GitHub.
    private var keysSha: String? = null
    private var devicesSha: String? = null
    private var blockedSha: String? = null

    private var lastGoodKeysRaw: String = ""
    private var lastGoodDevicesRaw: String = ""
    private var lastGoodBlockedRaw: String = ""

    /** Fetch everything live; fall back to cache on any failure. Always updates cache on success. */
    suspend fun loadSnapshot(): RepoResult<RepoSnapshot> {
        return try {
            val keysFile = GitHubClient.fetchFile(GitHubConfig.KEYS_PATH)
            val devicesFile = GitHubClient.fetchFile(GitHubConfig.DEVICES_PATH)
            val blockedFile = GitHubClient.fetchFile(GitHubConfig.BLOCKED_PATH)

            keysSha = keysFile.sha
            devicesSha = devicesFile.sha
            blockedSha = blockedFile.sha
            lastGoodKeysRaw = keysFile.content
            lastGoodDevicesRaw = devicesFile.content
            lastGoodBlockedRaw = blockedFile.content

            cache.save(keysFile.content, devicesFile.content, blockedFile.content)

            val snapshot = RepoSnapshot(
                keys = parseKeys(keysFile.content),
                devices = parseDevices(devicesFile.content),
                blocked = parseBlocked(blockedFile.content),
                fromCache = false,
                fetchedAt = System.currentTimeMillis(),
            )
            RepoResult.Success(snapshot, fromCache = false)
        } catch (e: Exception) {
            val cached = cache.load()
            if (cached != null) {
                lastGoodKeysRaw = cached.keysRaw
                lastGoodDevicesRaw = cached.devicesRaw
                lastGoodBlockedRaw = cached.blockedRaw
                val snapshot = RepoSnapshot(
                    keys = parseKeys(cached.keysRaw),
                    devices = parseDevices(cached.devicesRaw),
                    blocked = parseBlocked(cached.blockedRaw),
                    fromCache = true,
                    fetchedAt = cached.cachedAt,
                )
                RepoResult.Success(snapshot, fromCache = true)
            } else {
                RepoResult.Failure(e.message ?: "Failed to load data and no cache available.")
            }
        }
    }

    // ============ KEY OPERATIONS ============

    suspend fun generateKey(durationInput: String, deviceLimit: Int, customText: String? = null): RepoResult<KeyRecord> {
        val now = RealTimeProvider.nowMillis()
        val expiryResult = DurationUtil.computeExpiry(durationInput, now)
        if (expiryResult is com.authmanager.app.util.DurationResult.Error) {
            return RepoResult.Failure(expiryResult.message)
        }
        val expiry = (expiryResult as com.authmanager.app.util.DurationResult.Success).expiry
        val created = DurationUtil.nowFormatted(now)
        val key = customText ?: generateRandomKey()

        return try {
            val keysFile = GitHubClient.fetchFile(GitHubConfig.KEYS_PATH)
            val existing = parseKeys(keysFile.content)
            if (existing.any { it.key == key }) {
                return RepoResult.Failure("A key with this exact text already exists.")
            }
            val record = KeyRecord(key, expiry, deviceLimit, created)
            val newContent = keysFile.content.trimEnd('\n') + "\n" + record.toLine() + "\n"
            GitHubClient.writeFile(GitHubConfig.KEYS_PATH, newContent, keysFile.sha, "generate key $key")
            keysSha = null // force fresh sha next fetch
            RepoResult.Success(record, fromCache = false)
        } catch (e: Exception) {
            RepoResult.Failure(e.message ?: "Failed to generate key.")
        }
    }

    suspend fun deleteKey(key: String): RepoResult<Int> {
        return try {
            val keysFile = GitHubClient.fetchFile(GitHubConfig.KEYS_PATH)
            val remaining = keysFile.content.lines().filter { it.isNotBlank() && it.substringBefore("|") != key }
            val newKeysContent = remaining.joinToString("\n") + if (remaining.isNotEmpty()) "\n" else ""
            GitHubClient.writeFile(GitHubConfig.KEYS_PATH, newKeysContent, keysFile.sha, "delete key $key")

            val devicesFile = GitHubClient.fetchFile(GitHubConfig.DEVICES_PATH)
            val allDeviceLines = devicesFile.content.lines().filter { it.isNotBlank() }
            val remainingDevices = allDeviceLines.filter { it.substringBefore("|") != key }
            val removedCount = allDeviceLines.size - remainingDevices.size
            val newDevicesContent = remainingDevices.joinToString("\n") + if (remainingDevices.isNotEmpty()) "\n" else ""
            GitHubClient.writeFile(GitHubConfig.DEVICES_PATH, newDevicesContent, devicesFile.sha, "cascade-delete devices for key $key")

            RepoResult.Success(removedCount, fromCache = false)
        } catch (e: Exception) {
            RepoResult.Failure(e.message ?: "Failed to delete key.")
        }
    }

    suspend fun changeKeyDuration(key: String, durationInput: String): RepoResult<String> {
        val now = RealTimeProvider.nowMillis()
        val expiryResult = DurationUtil.computeExpiry(durationInput, now)
        if (expiryResult is com.authmanager.app.util.DurationResult.Error) {
            return RepoResult.Failure(expiryResult.message)
        }
        val newExpiry = (expiryResult as com.authmanager.app.util.DurationResult.Success).expiry

        return try {
            val keysFile = GitHubClient.fetchFile(GitHubConfig.KEYS_PATH)
            var found = false
            val updatedLines = keysFile.content.lines().filter { it.isNotBlank() }.map { line ->
                val parts = line.split("|").toMutableList()
                if (parts.isNotEmpty() && parts[0] == key && parts.size >= 4) {
                    found = true
                    parts[1] = newExpiry
                    parts.joinToString("|")
                } else line
            }
            if (!found) return RepoResult.Failure("Key no longer exists.")
            val newContent = updatedLines.joinToString("\n") + "\n"
            GitHubClient.writeFile(GitHubConfig.KEYS_PATH, newContent, keysFile.sha, "change duration for key $key")
            RepoResult.Success(newExpiry, fromCache = false)
        } catch (e: Exception) {
            RepoResult.Failure(e.message ?: "Failed to update duration.")
        }
    }

    // ============ DEVICE OPERATIONS ============

    suspend fun registerDevice(key: String, hash: String): RepoResult<DeviceRecord> {
        return try {
            val keysFile = GitHubClient.fetchFile(GitHubConfig.KEYS_PATH)
            val record = parseKeys(keysFile.content).find { it.key == key }
                ?: return RepoResult.Failure("Key no longer exists.")

            val now = RealTimeProvider.nowMillis()
            if (DurationUtil.isExpired(record.expiry, now)) {
                return RepoResult.Failure("Key is expired. Cannot register.")
            }

            val devicesFile = GitHubClient.fetchFile(GitHubConfig.DEVICES_PATH)
            val existing = parseDevices(devicesFile.content)
            val forKey = existing.filter { it.key == key }

            if (forKey.any { it.hash == hash }) {
                return RepoResult.Failure("This device is already registered to this key.")
            }
            if (record.deviceLimit != 0 && forKey.size >= record.deviceLimit) {
                return RepoResult.Failure("Key has reached its device limit (${forKey.size}/${record.deviceLimit}).")
            }

            val registered = DurationUtil.nowFormatted(now)
            val newDevice = DeviceRecord(key, hash, "N/A", "N/A", "N/A", registered)
            val newContent = devicesFile.content.trimEnd('\n') + "\n" + newDevice.toLine() + "\n"
            GitHubClient.writeFile(GitHubConfig.DEVICES_PATH, newContent, devicesFile.sha, "register device to key $key")
            RepoResult.Success(newDevice, fromCache = false)
        } catch (e: Exception) {
            RepoResult.Failure(e.message ?: "Failed to register device.")
        }
    }

    suspend fun unregisterDevice(hash: String): RepoResult<Unit> {
        return try {
            val devicesFile = GitHubClient.fetchFile(GitHubConfig.DEVICES_PATH)
            val before = devicesFile.content.lines().filter { it.isNotBlank() }
            val remaining = before.filter { line ->
                val parts = line.split("|")
                parts.size < 2 || parts[1] != hash
            }
            if (remaining.size == before.size) {
                return RepoResult.Failure("No device found with that hash.")
            }
            val newContent = remaining.joinToString("\n") + if (remaining.isNotEmpty()) "\n" else ""
            GitHubClient.writeFile(GitHubConfig.DEVICES_PATH, newContent, devicesFile.sha, "unregister device $hash")
            RepoResult.Success(Unit, fromCache = false)
        } catch (e: Exception) {
            RepoResult.Failure(e.message ?: "Failed to unregister device.")
        }
    }

    suspend fun blockDevice(hash: String): RepoResult<Unit> {
        return try {
            val blockedFile = GitHubClient.fetchFile(GitHubConfig.BLOCKED_PATH)
            val existing = parseBlocked(blockedFile.content)
            if (existing.any { it.hash == hash }) {
                return RepoResult.Failure("Already blocked.")
            }
            val now = RealTimeProvider.nowMillis()
            val newEntry = BlockedRecord(hash, DurationUtil.nowFormatted(now))
            val newContent = blockedFile.content.trimEnd('\n') + "\n" + newEntry.toLine() + "\n"
            GitHubClient.writeFile(GitHubConfig.BLOCKED_PATH, newContent, blockedFile.sha, "block device $hash")
            RepoResult.Success(Unit, fromCache = false)
        } catch (e: Exception) {
            RepoResult.Failure(e.message ?: "Failed to block device.")
        }
    }

    suspend fun unblockDevice(hash: String): RepoResult<Unit> {
        return try {
            val blockedFile = GitHubClient.fetchFile(GitHubConfig.BLOCKED_PATH)
            val before = blockedFile.content.lines().filter { it.isNotBlank() }
            val remaining = before.filter { line -> line.substringBefore("|") != hash }
            if (remaining.size == before.size) {
                return RepoResult.Failure("Device wasn't blocked.")
            }
            val newContent = remaining.joinToString("\n") + if (remaining.isNotEmpty()) "\n" else ""
            GitHubClient.writeFile(GitHubConfig.BLOCKED_PATH, newContent, blockedFile.sha, "unblock device $hash")
            RepoResult.Success(Unit, fromCache = false)
        } catch (e: Exception) {
            RepoResult.Failure(e.message ?: "Failed to unblock device.")
        }
    }

    // ============ PARSING HELPERS ============

    private fun parseKeys(raw: String): List<KeyRecord> =
        raw.lines().filter { it.isNotBlank() }.mapNotNull { KeyRecord.fromLine(it) }

    private fun parseDevices(raw: String): List<DeviceRecord> =
        raw.lines().filter { it.isNotBlank() }.mapNotNull { DeviceRecord.fromLine(it) }

    private fun parseBlocked(raw: String): List<BlockedRecord> =
        raw.lines().filter { it.isNotBlank() }.mapNotNull { BlockedRecord.fromLine(it) }

    private fun generateRandomKey(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val body = (1..20).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
        return "AM-$body"
    }
}
