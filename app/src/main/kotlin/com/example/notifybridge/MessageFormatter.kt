package com.example.notifybridge

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MessageFormatter {

    fun format(bankName: String, title: String, text: String): String {
        val now  = LocalDateTime.now()
        val time = now.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))

        val combined = "$title $text"
        val isDeposit = combined.contains("입금") || combined.contains("받기")
        val emoji = if (isDeposit) "💰" else "💸"
        val type  = if (isDeposit) "입금" else "출금"

        val amount     = parseAmount(title, text)
        val counterpart = parseCounterpart(bankName, title, text, isDeposit)
        val label      = if (isDeposit) "보낸이" else "받는이"

        val bankEmoji = bankEmoji(bankName)

        return """
$emoji $bankEmoji *$bankName $type 알림*
━━━━━━━━━━━━━━
💵 금액 : $amount
👤 $label : $counterpart
⏰ 시각 : $time
━━━━━━━━━━━━━━
📄 원문 : $text
        """.trimIndent()
    }

    // ── 금액 추출 ─────────────────────────────────────────────────
    private fun parseAmount(title: String, text: String): String {
        val regex = Regex("""[\d,]+원""")
        return regex.find(title)?.value
            ?: regex.find(text)?.value
            ?: "금액 미확인"
    }

    // ── 은행별 상대방 이름 추출 ───────────────────────────────────
    private fun parseCounterpart(
        bankName: String, title: String, text: String, isDeposit: Boolean
    ): String {
        return when (bankName) {

            // 카카오뱅크: "이준영 → 내 mini"
            "카카오뱅크" -> {
                val arrow = Regex("""(.+?)\s*→\s*(.+)""").find(text)
                if (arrow != null) {
                    if (isDeposit) arrow.groupValues[1].trim()
                    else           arrow.groupValues[2].trim()
                } else parenName(text)
            }

            // 케이뱅크: "(홍길동)님으로부터 입금" / "출금(홍길동)"
            "케이뱅크" -> {
                parenName(text) ?: nameBeforeKeyword(text)
            }

            // 농협: "홍길동님으로부터" / "홍길동에게"
            "농협은행" -> {
                Regex("""(.+?)(?:님으로부터|으로부터|에게|님께)""").find(text)
                    ?.groupValues?.get(1)?.trim()
                    ?: parenName(text)
                    ?: nameBeforeKeyword(text)
            }

            // 우리은행: "(홍길동)" 또는 "홍길동 →" 형식
            "우리은행" -> {
                val arrow = Regex("""(.+?)\s*→""").find(text)
                arrow?.groupValues?.get(1)?.trim()
                    ?: parenName(text)
                    ?: nameBeforeKeyword(text)
            }

            else -> parenName(text) ?: nameBeforeKeyword(text) ?: "정보없음"
        } ?: "정보없음"
    }

    private fun parenName(text: String): String? =
        Regex("""\(([^)]+)\)""").find(text)?.groupValues?.get(1)?.trim()

    private fun nameBeforeKeyword(text: String): String? =
        Regex("""^(.{1,10}?)\s*(?:입금|출금|이체|결제)""").find(text)
            ?.groupValues?.get(1)?.trim()

    private fun bankEmoji(bankName: String) = when (bankName) {
        "카카오뱅크" -> "🟡"
        "케이뱅크"   -> "🟣"
        "농협은행"   -> "🟢"
        "우리은행"   -> "🔵"
        else         -> "🏦"
    }
}
