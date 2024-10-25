package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class DiagnosisA1Activity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_a1)

        // 3초 후 DiagnosisA2Activity로 전환
        handler.postDelayed({
            startActivity(Intent(this, DiagnosisA2Activity::class.java))
            finish()
        }, 3000) // 3초 대기
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}