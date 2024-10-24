package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class AnalysisMainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analysis_main)

        // 5초 후 analysis_result2로 전환
        handler.postDelayed({
            startActivity(Intent(this, AnalysisResultActivity::class.java))
        }, 5000)
    }
}
