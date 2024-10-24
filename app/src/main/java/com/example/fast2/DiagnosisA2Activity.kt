package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DiagnosisA2Activity : AppCompatActivity() {
    private lateinit var countdownText: TextView
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_a2)

        countdownText = findViewById(R.id.COUNT_text)

        startCountdown()
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                countdownText.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                startActivity(Intent(this@DiagnosisA2Activity, DiagnosisS1Activity::class.java))
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}