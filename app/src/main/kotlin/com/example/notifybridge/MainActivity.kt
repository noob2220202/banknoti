package com.example.notifybridge

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 라이선스 없으면 LicenseActivity로 이동
        if (!LicenseManager.isValid(this)) {
            startActivity(Intent(this, LicenseActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        TelegramSender.init(this)

        findViewById<Button>(R.id.permButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.licenseButton).setOnClickListener {
            startActivity(Intent(this, LicenseActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // 라이선스 만료 시 onResume에서도 차단
        if (!LicenseManager.isValid(this)) {
            startActivity(Intent(this, LicenseActivity::class.java))
            finish()
            return
        }
        updateStatus()
    }

    private fun updateStatus() {
        val statusText = findViewById<TextView>(R.id.statusText)
        val permGranted = isNotificationPermissionGranted()
        val configured  = TelegramSender.isConfigured(this)
        val licInfo     = LicenseManager.getInfo(this)

        val permLine = if (permGranted) "✅ 알림 접근 권한 활성화됨" else "❌ 알림 접근 권한 필요"
        val botLine  = if (configured)  "✅ 텔레그램 봇 설정 완료"  else "⚠️ 텔레그램 봇 미설정"
        val licLine  = if (licInfo != null) {
            val expDate = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
                .format(Date(licInfo.expiryEpoch * 1000))
            "🔑 라이선스: ${licInfo.daysRemaining}일 남음 (~$expDate)"
        } else "🔑 라이선스 없음"

        statusText.text = "$permLine\n$botLine\n$licLine"

        findViewById<Button>(R.id.permButton).text =
            if (permGranted) "권한 설정 재확인" else "권한 허용하러 가기"
    }

    private fun isNotificationPermissionGranted(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(packageName)
    }
}
