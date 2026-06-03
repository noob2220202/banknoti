package com.example.notifybridge

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val permButton = findViewById<Button>(R.id.permButton)

        updateStatus(statusText, permButton)

        permButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        val permButton = findViewById<Button>(R.id.permButton)
        updateStatus(statusText, permButton)
    }

    private fun updateStatus(statusText: TextView, permButton: Button) {
        if (isNotificationPermissionGranted()) {
            statusText.text = "✅ 알림 접근 권한 활성화됨\n카카오뱅크 알림 감지 중..."
            permButton.text = "권한 설정 재확인"
        } else {
            statusText.text = "❌ 알림 접근 권한 필요\n아래 버튼을 눌러 권한을 허용해주세요."
            permButton.text = "권한 허용하러 가기"
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(packageName)
    }
}
