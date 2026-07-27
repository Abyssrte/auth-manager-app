package com.authmanager.app.util

import android.os.Build
import java.security.MessageDigest

/**
 * Computes this device's identity hash. The Python side reads getprop values
 * (ro.boot.hardware, ro.product.brand, ro.product.model) via Termux; on stock
 * Android those map to Build.BOARD, Build.BRAND, Build.MODEL, so the same
 * "hardware-brand-model" string is hashed the same way — the two schemes only
 * match if both are run against the same physical device. This app's own login
 * check is independent: it compares against ADMIN_HASH_PREFIX configured for
 * this build.
 */
object DeviceHash {

    fun compute(): String {
        val raw = "${Build.BOARD}-${Build.BRAND}-${Build.MODEL}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computePrefix(length: Int = 10): String = compute().take(length)
}
