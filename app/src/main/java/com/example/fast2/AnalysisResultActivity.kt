package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class AnalysisMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analysis_main)

        // 3초 후 결과 화면으로 전환
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, AnalysisResultActivity::class.java))
            finish()
        }, 3000)
    }
}