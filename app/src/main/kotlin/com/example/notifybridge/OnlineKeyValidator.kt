package com.example.notifybridge

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OnlineKeyValidator {

    private const val TAG = "KeyValidator"
    private const val SERVER_URL = "http://119.205.221.171:6000"

    enum class Result {
        ACTIVATED,
        ALREADY_USED,
        NOT_FOUND,
        OFFLINE_OK,
        ERROR
    }

    fun activate(context: Context, licenseKey: String): Result {
        val saved = LicenseManager.getSavedKey(context)
        if (saved?.trim() == licenseKey.trim() && LicenseManager.isValid(context)) {
            return Result.OFFLINE_OK
        }

        return try {
            val conn = URL("$SERVER_URL/api/activate").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"

            val body = JSONObject().apply {
                put("key", licenseKey.trim())
                put("device_id", deviceId)
            }.toString()
            conn.outputStream.write(body.toByteArray())

            val code = conn.responseCode
            val response = when {
                code in 200..299 -> conn.inputStream.bufferedReader().readText()
                else             -> conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()
            Log.d(TAG, "activate $code → $response")

            when (code) {
                200 -> {
                    val json = JSONObject(response)
                    val expiresAt = json.getLong("expires_at")
                    LicenseManager.save(context, "ONLINE:${licenseKey.trim()}:$expiresAt")
                    Result.ACTIVATED
                }
                404 -> Result.NOT_FOUND
                409 -> Result.ALREADY_USED
                else -> Result.ERROR
            }
        } catch (e: Exception) {
            Log.e(TAG, "에러: ${e.message}")
            Result.ERROR
        }
    }

    fun isConfigured(context: Context) = true
}
