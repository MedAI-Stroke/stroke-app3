package com.example.fast2

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class DiagnosisF1Activity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_f1)

        // MediaPlayer 초기화 및 mp3 파일 설정
        mediaPlayer = MediaPlayer.create(this, R.raw.f_start)

        // 음성 파일 재생
        mediaPlayer.start()

        // 음성 파일이 재생된 후 1초 뒤에 다음 화면으로 전환
        mediaPlayer.setOnCompletionListener {
            handler.postDelayed({
                startActivity(Intent(this, DiagnosisF2Activity::class.java))
                finish() // 현재 Activity 종료
            }, 1000) // 1초 대기
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)

        // MediaPlayer 해제
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
