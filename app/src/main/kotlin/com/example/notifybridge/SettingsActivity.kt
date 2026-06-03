package com.example.notifybridge

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.title = "텔레그램 봇 설정"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tokenInput  = findViewById<EditText>(R.id.inputBotToken)
        val chatIdInput = findViewById<EditText>(R.id.inputChatId)
        val saveButton  = findViewById<Button>(R.id.btnSave)
        val testButton  = findViewById<Button>(R.id.btnTest)

        // 저장된 값 불러오기
        val prefs = getSharedPreferences("notifybridge_prefs", Context.MODE_PRIVATE)
        tokenInput.setText(prefs.getString(TelegramSender.KEY_BOT_TOKEN, ""))
        chatIdInput.setText(prefs.getString(TelegramSender.KEY_CHAT_ID, ""))

        saveButton.setOnClickListener {
            val token  = tokenInput.text.toString().trim()
            val chatId = chatIdInput.text.toString().trim()

            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "봇 토큰과 채팅 ID를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString(TelegramSender.KEY_BOT_TOKEN, token)
                .putString(TelegramSender.KEY_CHAT_ID, chatId)
                .apply()

            Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show()
        }

        testButton.setOnClickListener {
            val token  = tokenInput.text.toString().trim()
            val chatId = chatIdInput.text.toString().trim()

            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "먼저 저장을 눌러주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "테스트 메시지 전송 중...", Toast.LENGTH_SHORT).show()

            Thread {
                TelegramSender.send("✅ *NotifyBridge 연결 테스트*\n카카오뱅크 알림 전송이 정상 작동합니다!")
                runOnUiThread {
                    Toast.makeText(this, "전송 완료! 텔레그램을 확인해주세요.", Toast.LENGTH_LONG).show()
                }
            }.start()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
