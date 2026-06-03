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

        supportActionBar?.title = "설정"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs       = getSharedPreferences("notifybridge_prefs", Context.MODE_PRIVATE)
        val tokenInput  = findViewById<EditText>(R.id.inputBotToken)
        val chatIdInput = findViewById<EditText>(R.id.inputChatId)
        val serverInput = findViewById<EditText>(R.id.inputServerUrl)
        val saveButton  = findViewById<Button>(R.id.btnSave)
        val testButton  = findViewById<Button>(R.id.btnTest)

        tokenInput.setText(prefs.getString(TelegramSender.KEY_BOT_TOKEN, ""))
        chatIdInput.setText(prefs.getString(TelegramSender.KEY_CHAT_ID, ""))
        serverInput.setText(prefs.getString(OnlineKeyValidator.KEY_SERVER_URL, ""))

        saveButton.setOnClickListener {
            val token     = tokenInput.text.toString().trim()
            val chatId    = chatIdInput.text.toString().trim()
            val serverUrl = serverInput.text.toString().trim().trimEnd('/')

            if (token.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "봇 토큰과 채팅 ID를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString(TelegramSender.KEY_BOT_TOKEN, token)
                .putString(TelegramSender.KEY_CHAT_ID, chatId)
                .putString(OnlineKeyValidator.KEY_SERVER_URL, serverUrl)
                .apply()

            Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show()
        }

        testButton.setOnClickListener {
            if (tokenInput.text.isBlank() || chatIdInput.text.isBlank()) {
                Toast.makeText(this, "먼저 저장을 눌러주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "테스트 메시지 전송 중...", Toast.LENGTH_SHORT).show()
            Thread {
                TelegramSender.send("✅ *NotifyBridge 연결 테스트*\n알림 전송이 정상 작동합니다!")
                runOnUiThread {
                    Toast.makeText(this, "전송 완료!", Toast.LENGTH_LONG).show()
                }
            }.start()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
