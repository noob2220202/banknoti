package com.example.notifybridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        supportActionBar?.title = "라이선스 인증"

        val statusText  = findViewById<TextView>(R.id.licenseStatusText)
        val keyInput    = findViewById<EditText>(R.id.inputLicenseKey)
        val activateBtn = findViewById<Button>(R.id.btnActivate)
        val continueBtn = findViewById<Button>(R.id.btnContinue)
        val progress    = findViewById<ProgressBar>(R.id.licenseProgress)

        updateStatus(statusText, continueBtn)

        activateBtn.setOnClickListener {
            val key = keyInput.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "라이선스 키를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            activateBtn.isEnabled = false
            progress.visibility = View.VISIBLE

            Thread {
                val result = OnlineKeyValidator.activate(this, key)
                runOnUiThread {
                    progress.visibility = View.GONE
                    activateBtn.isEnabled = true
                    when (result) {
                        OnlineKeyValidator.Result.ACTIVATED -> {
                            keyInput.text.clear()
                            updateStatus(statusText, continueBtn)
                            Toast.makeText(this, "✅ 라이선스 활성화 완료!", Toast.LENGTH_LONG).show()
                        }
                        OnlineKeyValidator.Result.OFFLINE_OK -> {
                            updateStatus(statusText, continueBtn)
                            Toast.makeText(this, "✅ 이미 이 기기에서 활성화된 키입니다.", Toast.LENGTH_SHORT).show()
                        }
                        OnlineKeyValidator.Result.ALREADY_USED ->
                            Toast.makeText(this, "❌ 이미 다른 기기에서 사용된 키입니다.", Toast.LENGTH_LONG).show()
                        OnlineKeyValidator.Result.NOT_FOUND ->
                            Toast.makeText(this, "❌ 존재하지 않는 키입니다.", Toast.LENGTH_LONG).show()
                        OnlineKeyValidator.Result.NOT_CONFIGURED ->
                            Toast.makeText(this, "⚠️ 서버 URL이 설정되지 않았습니다.\n설정 화면에서 입력해주세요.", Toast.LENGTH_LONG).show()
                        OnlineKeyValidator.Result.ERROR ->
                            Toast.makeText(this, "⚠️ 서버 연결 실패. 인터넷 확인 후 재시도하세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        continueBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.btnGoSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus(
            findViewById(R.id.licenseStatusText),
            findViewById(R.id.btnContinue)
        )
    }

    private fun updateStatus(statusText: TextView, continueBtn: Button) {
        val info = LicenseManager.getInfo(this)
        val configured = OnlineKeyValidator.isConfigured(this)
        val serverLine = if (configured) "🌐 서버 연결됨" else "⚠️ 서버 URL 미설정"

        if (info != null && !info.isExpired) {
            val expDate = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                .format(Date(info.expiryEpoch * 1000))
            statusText.text = "✅ 라이선스 유효\n만료일: $expDate (${info.daysRemaining}일 남음)\n$serverLine"
            statusText.setTextColor(0xFF2E7D32.toInt())
            continueBtn.isEnabled = true
            continueBtn.alpha = 1f
        } else if (info != null && info.isExpired) {
            statusText.text = "⚠️ 라이선스 만료됨\n새 키를 입력해주세요.\n$serverLine"
            statusText.setTextColor(0xFFB71C1C.toInt())
            continueBtn.isEnabled = false
            continueBtn.alpha = 0.4f
        } else {
            statusText.text = "❌ 라이선스 없음\n키를 입력하여 활성화하세요.\n$serverLine"
            statusText.setTextColor(0xFFB71C1C.toInt())
            continueBtn.isEnabled = false
            continueBtn.alpha = 0.4f
        }
    }
}
