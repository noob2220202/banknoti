package com.example.notifybridge

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OnlineKeyValidator {

    private const val TAG = "KeyValidator"
    private const val PREFS = "notifybridge_prefs"
    const val KEY_SERVER_URL = "license_server_url"

    enum class Result {
        ACTIVATED,      // 활성화 성공 (키 소모됨)
        ALREADY_USED,   // 이미 사용된 키
        NOT_FOUND,      // 존재하지 않는 키
        OFFLINE_OK,     // 이 기기에서 이미 활성화된 키
        NOT_CONFIGURED, // 서버 URL 미설정
        ERROR           // 네트워크/서버 오류
    }

    fun activate(context: Context, licenseKey: String): Result {
        // 이미 이 기기에서 활성화된 키면 바로 허용
        val saved = LicenseManager.getSavedKey(context)
        if (saved?.trim() == licenseKey.trim() && LicenseManager.isValid(context)) {
            return Result.OFFLINE_OK
        }

        val serverUrl = getServerUrl(context)
        if (serverUrl.isEmpty()) return Result.NOT_CONFIGURED

        return try {
            val conn = URL("$serverUrl/api/activate").openConnection() as HttpURLConnection
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
                200  -> {
                    val json = JSONObject(response)
                    val expiresAt = json.getLong("expires_at")
                    // 만료 시각을 로컬 키 형식으로 저장
                    val localKey = buildLocalKey(licenseKey, expiresAt)
                    LicenseManager.save(context, localKey)
                    Result.ACTIVATED
                }
                404  -> Result.NOT_FOUND
                409  -> Result.ALREADY_USED
                else -> Result.ERROR
            }
        } catch (e: Exception) {
            Log.e(TAG, "에러: ${e.message}")
            Result.ERROR
        }
    }

    private fun buildLocalKey(originalKey: String, expiresAt: Long): String {
        // 서버 검증 완료 후 로컬 저장용 키 = "ONLINE:{originalKey}:{expiresAt}"
        return "ONLINE:${originalKey.trim()}:$expiresAt"
    }

    fun getServerUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SERVER_URL, "")?.trim() ?: ""

    fun isConfigured(context: Context) = getServerUrl(context).isNotEmpty()
}
