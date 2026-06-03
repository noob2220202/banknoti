package com.example.notifybridge

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MessageFormatter {

    fun format(title: String, text: String): String {
        val now = LocalDateTime.now()
        val time = now.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))

        val isDeposit = title.contains("입금") || text.contains("입금")
        val emoji = if (isDeposit) "💰" else "💸"
        val type  = if (isDeposit) "입금" else "출금"

        val amountRegex = Regex("""[\d,]+원""")
        val amount = amountRegex.find(text)?.value ?: "금액 미확인"

        val nameRegex = Regex("""\(([^)]+)\)""")
        val name = nameRegex.find(text)?.groupValues?.get(1) ?: "정보없음"

        return """
$emoji *카카오뱅크 $type 알림*
━━━━━━━━━━━━━━
💵 금액 : $amount
👤 ${if (isDeposit) "보낸이" else "받는이"} : $name
⏰ 시각 : $time
━━━━━━━━━━━━━━
📄 원문 : $text
        """.trimIndent()
    }
}
