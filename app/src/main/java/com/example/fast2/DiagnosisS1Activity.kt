package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class DiagnosisS1Activity : AppCompatActivity() {
    private val handler =  Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_s1)

        // 3초 후 DiagnosisS2Activity로 전환
        handler.postDelayed({
            startActivity(Intent(this, DiagnosisS2Activity::class.java))
        }, 3000)  // 5초 대기
    }
}
