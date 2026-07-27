package com.authmanager.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val TIME_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
    timeZone = TimeZone.getDefault()
}

sealed class DurationResult {
    data class Success(val expiry: String) : DurationResult()
    data class Error(val message: String) : DurationResult()
}

object DurationUtil {

    /**
     * Parses '30d' / '5h' / '10m' / 'unlimited' and returns a formatted expiry
     * timestamp computed from `nowMillis` (pass real fetched time when available,
     * device time otherwise).
     */
    fun computeExpiry(input: String, nowMillis: Long): DurationResult {
        val text = input.trim().lowercase()
        if (text == "unlimited") return DurationResult.Success("unlimited")

        if (text.length < 2) return DurationResult.Error("Invalid format. Use a number + d/h/m, or 'unlimited'.")
        val unit = text.last()
        val numberPart = text.dropLast(1)
        val value = numberPart.toIntOrNull()
        if (value == null || value <= 0 || unit !in charArrayOf('d', 'h', 'm')) {
            return DurationResult.Error("Invalid format. Use a number + d/h/m — e.g. 30d, 5h, 10m, unlimited.")
        }

        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        when (unit) {
            'd' -> cal.add(Calendar.DAY_OF_MONTH, value)
            'h' -> cal.add(Calendar.HOUR_OF_DAY, value)
            'm' -> cal.add(Calendar.MINUTE, value)
        }
        return DurationResult.Success(TIME_FMT.format(cal.time))
    }

    /** Human-readable remaining time (or EXPIRED/unlimited) for list displays. */
    fun timeLeftLabel(expiry: String, nowMillis: Long): String {
        if (expiry.trim().lowercase() == "unlimited") return "unlimited"
        val expiryMillis = try {
            TIME_FMT.parse(expiry)?.time
        } catch (e: Exception) {
            null
        } ?: return "unknown"

        val remainingSeconds = (expiryMillis - nowMillis) / 1000
        if (remainingSeconds <= 0) return "EXPIRED"

        val days = remainingSeconds / 86400
        val hours = (remainingSeconds % 86400) / 3600
        val minutes = (remainingSeconds % 3600) / 60

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0 && days == 0L) parts.add("${minutes}m")
        return if (parts.isEmpty()) "<1m" else parts.joinToString(" ")
    }

    fun isExpired(expiry: String, nowMillis: Long): Boolean {
        if (expiry.trim().lowercase() == "unlimited") return false
        val expiryMillis = try {
            TIME_FMT.parse(expiry)?.time
        } catch (e: Exception) {
            null
        } ?: return false
        return expiryMillis < nowMillis
    }

    /** True when a key expires within the next 24 hours (but hasn't expired yet) — used to flag it yellow in the UI. */
    fun isUrgent(expiry: String, nowMillis: Long): Boolean {
        if (expiry.trim().lowercase() == "unlimited") return false
        val expiryMillis = try {
            TIME_FMT.parse(expiry)?.time
        } catch (e: Exception) {
            null
        } ?: return false
        val remainingMillis = expiryMillis - nowMillis
        return remainingMillis in 1..(24 * 60 * 60 * 1000L)
    }

    fun nowFormatted(nowMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        return TIME_FMT.format(cal.time)
    }
}
