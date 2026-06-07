package com.example.notifybridge

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    companion object {
        const val TAG = "NotifyBridge"

        // 지원 은행 패키지명 → 표시 이름
        val BANK_PACKAGES = mapOf(
            "com.kakaobank.channel"     to "카카오뱅크",
            "com.kbankwith.smartbank"   to "케이뱅크",
            "nh.smart.banking"          to "농협은행",
            "com.wooribank.smart.won"   to "우리은행"
        )

        val TRANSACTION_KEYWORDS = listOf("입금", "출금", "이체", "결제", "원")
    }

    override fun onCreate() {
        super.onCreate()
        TelegramSender.init(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val bankName = BANK_PACKAGES[sbn.packageName] ?: return

        val extras = sbn.notification.extras
        val title  = extras.getString("android.title") ?: ""
        val text   = extras.getString("android.text")  ?: ""

        Log.d(TAG, "[$bankName] 알림 감지 - title: $title / text: $text")

        if (!isTransactionNotification(title, text)) return

        val formatted = MessageFormatter.format(bankName, title, text)

        Thread { TelegramSender.send(formatted) }.start()
    }

    private fun isTransactionNotification(title: String, text: String): Boolean =
        TRANSACTION_KEYWORDS.any { title.contains(it) || text.contains(it) }
}
