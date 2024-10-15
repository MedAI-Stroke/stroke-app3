package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class DiagnosisS2Activity : AppCompatActivity() {
    private val SPEECH_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_s2)

        // 3초 후 음성 인식 시작
        Handler().postDelayed({
            captureSpeech()
        }, 3000)
    }

    private fun captureSpeech() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        startActivityForResult(speechIntent, SPEECH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            // 5초 후 AnalysisMainActivity로 전환
            Handler().postDelayed({
                startActivity(Intent(this, AnalysisMainActivity::class.java))
            }, 5000)
        }
    }
}
