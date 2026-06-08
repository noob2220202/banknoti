package com.example.notifybridge

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    companion object {
        const val TAG = "NotifyBridge"
        const val KAKAO_BANK_PACKAGE = "com.kakaobank.channel"
    }

    override fun onCreate() {
        super.onCreate()
        TelegramSender.init(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != KAKAO_BANK_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text  = extras.getString("android.text")  ?: ""

        Log.d(TAG, "알림 감지 - title: $title / text: $text")

        if (!isTransactionNotification(title, text)) return

        val formatted = MessageFormatter.format(title, text)

        Thread {
            TelegramSender.send(formatted)
        }.start()
    }

    private fun isTransactionNotification(title: String, text: String): Boolean {
        val keywords = listOf("입금", "출금", "이체", "결제", "원")
        return keywords.any { title.contains(it) || text.contains(it) }
    }
}
