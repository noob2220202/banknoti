package com.example.notifybridge

import android.content.Context
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LicenseManager {

    private const val PREFS = "notifybridge_prefs"
    private const val KEY_LICENSE = "license_key"

    // 라이선스 키 생성 스크립트(generate_license.py)와 반드시 동일해야 함
    private const val SECRET = "NB-NotifyBridge-2024-K9xP2mQr"

    data class LicenseInfo(
        val expiryEpoch: Long,
        val daysRemaining: Long,
        val isExpired: Boolean
    )

    // 키 형식: {expiryEpoch}-{12자리 HMAC}
    fun validate(key: String): Boolean {
        val parts = key.trim().split("-")
        if (parts.size != 2) return false
        val expiry = parts[0].toLongOrNull() ?: return false
        val sig = parts[1]
        val expected = computeHmac(parts[0])
        return sig.equals(expected, ignoreCase = true)
            && expiry > System.currentTimeMillis() / 1000
    }

    fun save(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LICENSE, key.trim()).apply()
    }

    fun isValid(context: Context): Boolean {
        val key = getSavedKey(context) ?: return false
        return validate(key)
    }

    fun getInfo(context: Context): LicenseInfo? {
        val key = getSavedKey(context) ?: return null
        val parts = key.trim().split("-")
        if (parts.size != 2) return null
        val expiry = parts[0].toLongOrNull() ?: return null
        val now = System.currentTimeMillis() / 1000
        val remaining = maxOf(0, (expiry - now) / 86400)
        val isExpired = expiry <= now
        return LicenseInfo(expiry, remaining, isExpired)
    }

    fun getSavedKey(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LICENSE, null)

    private fun computeHmac(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .substring(0, 12)
            .uppercase()
    }
}
