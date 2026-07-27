package com.authmanager.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Fetches the current time from worldtimeapi.org so key-expiry math can't be fooled
 * by a wrong device clock. Falls back to the device's own clock if the network call
 * fails — mirrors get_real_now() in bot.py / auth.py so all three surfaces agree.
 */
object RealTimeProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    suspend fun nowMillis(): Long = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://worldtimeapi.org/api/ip")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext System.currentTimeMillis()
                val body = resp.body?.string() ?: return@withContext System.currentTimeMillis()
                val json = JSONObject(body)
                val raw = json.getString("datetime")
                    .substringBefore(".")
                    .substringBefore("+")
                    .substringBefore("Z")
                isoFormat.parse(raw)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
