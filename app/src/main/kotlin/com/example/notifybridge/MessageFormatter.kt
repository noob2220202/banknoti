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

        // 금액: title 또는 text에서 추출 (예: "입금 1원", "10,000원")
        val amountRegex = Regex("""[\d,]+원""")
        val amount = amountRegex.find(title)?.value
            ?: amountRegex.find(text)?.value
            ?: "금액 미확인"

        // 보낸이/받는이: "이준영 → 내 mini" 형식 파싱
        val arrowRegex = Regex("""(.+?)\s*→\s*(.+)""")
        val arrowMatch = arrowRegex.find(text)

        // 괄호 형식 "(이준영)" 도 함께 지원
        val parenRegex = Regex("""\(([^)]+)\)""")

        val sender: String
        val receiver: String

        if (arrowMatch != null) {
            sender   = arrowMatch.groupValues[1].trim()
            receiver = arrowMatch.groupValues[2].trim()
        } else {
            val parenName = parenRegex.find(text)?.groupValues?.get(1) ?: "정보없음"
            sender   = parenName
            receiver = parenName
        }

        val counterpart = if (isDeposit) sender else receiver
        val label       = if (isDeposit) "보낸이" else "받는이"

        return """
$emoji *카카오뱅크 $type 알림*
━━━━━━━━━━━━━━
💵 금액 : $amount
👤 $label : $counterpart
⏰ 시각 : $time
━━━━━━━━━━━━━━
📄 원문 : $text
        """.trimIndent()
    }
}
