package com.example.notifybridge

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramSender {

    // ⚠️ 본인 봇 토큰과 채팅 ID로 교체
    private const val BOT_TOKEN = "YOUR_BOT_TOKEN_HERE"
    private const val CHAT_ID   = "YOUR_CHAT_ID_HERE"

    private const val TAG = "TelegramSender"

    fun send(message: String) {
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val urlString = "https://api.telegram.org/bot$BOT_TOKEN" +
                "/sendMessage?chat_id=$CHAT_ID" +
                "&text=$encodedMsg&parse_mode=Markdown"

            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Log.d(TAG, "전송 성공")
            } else {
                Log.e(TAG, "전송 실패: $responseCode")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "에러: ${e.message}")
        }
    }
}
