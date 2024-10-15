package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class DiagnosisA2Activity : AppCompatActivity() {
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_a2)

        // 10초 후 DiagnosisS1Activity로 전환
        handler.postDelayed({
            startActivity(Intent(this, DiagnosisS1Activity::class.java))
        }, 10000)  // 10초 대기
    }
}
