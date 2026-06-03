package com.example.notifybridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

        updateStatus(statusText, continueBtn)

        activateBtn.setOnClickListener {
            val key = keyInput.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "라이선스 키를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (LicenseManager.validate(key)) {
                LicenseManager.save(this, key)
                keyInput.text.clear()
                updateStatus(statusText, continueBtn)
                Toast.makeText(this, "✅ 라이선스 활성화 완료!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "❌ 유효하지 않거나 만료된 키입니다.", Toast.LENGTH_LONG).show()
            }
        }

        continueBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun updateStatus(statusText: TextView, continueBtn: Button) {
        val info = LicenseManager.getInfo(this)
        if (info != null && !info.isExpired) {
            val expDate = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                .format(Date(info.expiryEpoch * 1000))
            statusText.text = "✅ 라이선스 유효\n만료일: $expDate (${info.daysRemaining}일 남음)"
            statusText.setTextColor(0xFF2E7D32.toInt())
            continueBtn.isEnabled = true
            continueBtn.alpha = 1f
        } else if (info != null && info.isExpired) {
            statusText.text = "⚠️ 라이선스 만료됨\n새 키를 입력해주세요."
            statusText.setTextColor(0xFFB71C1C.toInt())
            continueBtn.isEnabled = false
            continueBtn.alpha = 0.4f
        } else {
            statusText.text = "❌ 라이선스 없음\n키를 입력하여 활성화하세요."
            statusText.setTextColor(0xFFB71C1C.toInt())
            continueBtn.isEnabled = false
            continueBtn.alpha = 0.4f
        }
    }
}
