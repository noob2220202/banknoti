package com.example.notifybridge

import android.content.Context
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LicenseManager {

    private const val PREFS = "notifybridge_prefs"
    private const val KEY_LICENSE = "license_key"
    private const val SECRET = "NB-NotifyBridge-2024-K9xP2mQr"

    data class LicenseInfo(
        val expiryEpoch: Long,
        val daysRemaining: Long,
        val isExpired: Boolean,
        val isOnline: Boolean
    )

    fun isValid(context: Context): Boolean {
        val key = getSavedKey(context) ?: return false
        return when {
            key.startsWith("ONLINE:") -> validateOnlineKey(key)
            else                      -> validateOfflineKey(key)
        }
    }

    fun save(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LICENSE, key.trim()).apply()
    }

    fun getInfo(context: Context): LicenseInfo? {
        val key = getSavedKey(context) ?: return null
        return when {
            key.startsWith("ONLINE:") -> infoFromOnlineKey(key)
            else                      -> infoFromOfflineKey(key)
        }
    }

    fun getSavedKey(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LICENSE, null)

    // ── 오프라인 키 (generate_license.py로 생성) ──────────────────
    fun validateOfflineKey(key: String): Boolean {
        val parts = key.trim().split("-")
        if (parts.size != 2) return false
        val expiry = parts[0].toLongOrNull() ?: return false
        return parts[1].equals(computeHmac(parts[0]), ignoreCase = true)
            && expiry > System.currentTimeMillis() / 1000
    }

    private fun infoFromOfflineKey(key: String): LicenseInfo? {
        val parts = key.trim().split("-")
        if (parts.size != 2) return null
        val expiry = parts[0].toLongOrNull() ?: return null
        val now = System.currentTimeMillis() / 1000
        return LicenseInfo(
            expiryEpoch   = expiry,
            daysRemaining = maxOf(0, (expiry - now) / 86400),
            isExpired     = expiry <= now,
            isOnline      = false
        )
    }

    // ── 온라인 키 (VPS 서버에서 검증됨) ──────────────────────────
    private fun validateOnlineKey(key: String): Boolean {
        // 형식: ONLINE:{originalKey}:{expiresAt}
        val parts = key.split(":")
        if (parts.size != 3) return false
        val expiry = parts[2].toLongOrNull() ?: return false
        return expiry > System.currentTimeMillis() / 1000
    }

    private fun infoFromOnlineKey(key: String): LicenseInfo? {
        val parts = key.split(":")
        if (parts.size != 3) return null
        val expiry = parts[2].toLongOrNull() ?: return null
        val now = System.currentTimeMillis() / 1000
        return LicenseInfo(
            expiryEpoch   = expiry,
            daysRemaining = maxOf(0, (expiry - now) / 86400),
            isExpired     = expiry <= now,
            isOnline      = true
        )
    }

    private fun computeHmac(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .substring(0, 12).uppercase()
    }
}
