package com.authmanager.app.util

import android.os.Build
import java.security.MessageDigest

object DeviceHash {

    private fun getSystemProperty(key: String): String {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java)
            (getMethod.invoke(null, key) as? String).orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    fun compute(): String {
        val hardware = getSystemProperty("ro.boot.hardware")
        val brand = getSystemProperty("ro.product.brand")
        val model = getSystemProperty("ro.product.model")

        val raw = if (hardware.isBlank() && brand.isBlank() && model.isBlank()) {
            "${Build.BOARD}-${Build.BRAND}-${Build.MODEL}"
        } else {
            "$hardware-$brand-$model"
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computePrefix(length: Int = 10): String = compute().take(length)
}