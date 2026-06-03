package com.example.notifybridge

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramSender {

    private const val TAG = "TelegramSender"
    private const val PREFS_NAME = "notifybridge_prefs"
    const val KEY_BOT_TOKEN = "bot_token"
    const val KEY_CHAT_ID   = "chat_id"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun send(message: String) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val botToken = prefs.getString(KEY_BOT_TOKEN, "")?.trim() ?: ""
        val chatId   = prefs.getString(KEY_CHAT_ID,   "")?.trim() ?: ""

        if (botToken.isEmpty() || chatId.isEmpty()) {
            Log.w(TAG, "BOT_TOKEN 또는 CHAT_ID가 설정되지 않음")
            return
        }

        sendWithRetry(botToken, chatId, message, retryCount = 3)
    }

    private fun sendWithRetry(botToken: String, chatId: String, message: String, retryCount: Int) {
        var delayMs = 2000L
        repeat(retryCount) { attempt ->
            val success = sendOnce(botToken, chatId, message)
            if (success) return
            if (attempt < retryCount - 1) {
                Log.w(TAG, "재시도 대기 ${delayMs}ms (${attempt + 1}/$retryCount)")
                Thread.sleep(delayMs)
                delayMs *= 2
            }
        }
        Log.e(TAG, "최대 재시도 횟수($retryCount) 초과 - 전송 실패")
    }

    private fun sendOnce(botToken: String, chatId: String, message: String): Boolean {
        return try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val urlString = "https://api.telegram.org/bot$botToken" +
                "/sendMessage?chat_id=$chatId" +
                "&text=$encodedMsg&parse_mode=Markdown"

            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Log.d(TAG, "전송 성공")
                conn.disconnect()
                true
            } else {
                val body = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e(TAG, "전송 실패: HTTP $responseCode - $body")
                conn.disconnect()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "전송 에러: ${e.message}")
            false
        }
    }

    fun isConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_BOT_TOKEN, "")?.trim() ?: ""
        val chat  = prefs.getString(KEY_CHAT_ID,   "")?.trim() ?: ""
        return token.isNotEmpty() && chat.isNotEmpty()
    }
}
