
package com.example.fast2

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class AnalysisMainActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analysis_main)

        // finish.mp3 파일 재생
        mediaPlayer = MediaPlayer.create(this, R.raw.finish)
        mediaPlayer.start()

        // 음성 재생이 끝난 후 1초 뒤에 결과 화면으로 전환
        mediaPlayer.setOnCompletionListener {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, AnalysisResultActivity::class.java))
                finish()
            }, 1000) // 1초 대기 후 전환
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
