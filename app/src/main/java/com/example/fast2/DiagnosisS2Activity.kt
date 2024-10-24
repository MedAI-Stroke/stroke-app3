package com.example.fast2

import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class DiagnosisS2Activity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var outputFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_s2)

        setupRecorder()

        // 5초 동안 녹음 후 다음 화면으로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            stopRecording()
            startActivity(Intent(this, AnalysisMainActivity::class.java))
        }, 5000)
    }

    private fun setupRecorder() {
        outputFile = File(cacheDir, "audio_record.mp3")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
}