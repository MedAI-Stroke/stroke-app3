package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class DiagnosisF1Activity : AppCompatActivity() {
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_f1)

        // diagnosis_f2로 전환
        handler.postDelayed({
            startActivity(Intent(this, DiagnosisF2Activity::class.java))
        }, 3000)
    }
}
