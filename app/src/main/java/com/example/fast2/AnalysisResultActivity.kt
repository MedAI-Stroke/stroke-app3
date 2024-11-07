package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AnalysisResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 결과 데이터 가져오기
        val prefs = getSharedPreferences("analysis_results", MODE_PRIVATE)

        // 각 검사 결과와 점수 가져오기
        val faceStroke = prefs.getInt("face_stroke", 0)
        val leftArmStroke = prefs.getInt("left_arm_stroke", 0)
        val rightArmStroke = prefs.getInt("right_arm_stroke", 0)
        val speechStroke = prefs.getInt("speech_stroke", 0)


        // 디버그 모드 관련 변수
        var debugTapCount = 0
        val resetTapHandler = Handler(Looper.getMainLooper())
        val resetTapRunnable = Runnable { debugTapCount = 0 }


        // 진단 결과 텍스트 구성
        val resultBuilder = StringBuilder().apply {

            append("진단 결과\n\n")

            // 이상이 있는 경우 경고 메시지 추가
            if (faceStroke == 1 || leftArmStroke == 1 || rightArmStroke == 1 || speechStroke == 1) {
                setContentView(R.layout.analysis_result1)  // 첫 번째 결과 화면 사용
            } else {
                setContentView(R.layout.analysis_result2)  // 두 번째 결과 화면 사용
            }
        }


        // T 버튼 설정
        findViewById<ImageView>(R.id.T_button_image).apply {
            // 클릭 시 디버그 모드 진입
            setOnClickListener {
                debugTapCount++
                resetTapHandler.removeCallbacks(resetTapRunnable)
                resetTapHandler.postDelayed(resetTapRunnable, 3000) // 5초 내에 3번 탭해야 함

                if (debugTapCount >= 3) {
                    debugTapCount = 0
                    Toast.makeText(this@AnalysisResultActivity, "디버그 모드 진입", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AnalysisResultActivity, DebugActivity::class.java))
                }
            }

            // 길게 누르면 메인으로 돌아가기
            setOnLongClickListener {
                // SharedPreferences 초기화
                prefs.edit().clear().apply()

                // 새로운 태스크로 메인 액티비티 시작
                val intent = Intent(this@AnalysisResultActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
        }
    }
}
