package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class FActivity : AppCompatActivity() {
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_f)

        // diagnosis_f1으로 전환
        handler.postDelayed({
            startActivity(Intent(this, DiagnosisF1Activity::class.java))
        }, 3000)  // 3초 후 전환
    }
}
