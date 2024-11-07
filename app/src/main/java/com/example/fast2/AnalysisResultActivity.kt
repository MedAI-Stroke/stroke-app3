package com.example.fast2

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
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



        // 진단 결과 텍스트 구성
        val resultBuilder = StringBuilder().apply {

            append("진단 결과\n\n")

            // 이상이 있는 경우 경고 메시지 추가
            if (faceStroke == 1 || leftArmStroke == 1 || rightArmStroke == 1 ||speechStroke == 1) {
                setContentView(R.layout.analysis_result1)  // 첫 번째 결과 화면 사용
            } else {
                setContentView(R.layout.analysis_result2)  // 두 번째 결과 화면 사용
            }
        }
        
        // T 버튼 클릭 시 메인 화면으로 이동
        findViewById<ImageView>(R.id.T_button_image)?.setOnClickListener {
            // SharedPreferences 초기화
            prefs.edit().clear().apply()

            // 새로운 태스크로 메인 액티비티 시작
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}

