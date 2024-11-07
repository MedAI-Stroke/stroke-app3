package com.example.fast2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DebugActivity : AppCompatActivity() {
    private lateinit var faceResultText: TextView
    private lateinit var armResultText: TextView
    private lateinit var speechResultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.debug_view)

        // TextView 초기화
        faceResultText = findViewById(R.id.face_result)
        armResultText = findViewById(R.id.arm_result)
        speechResultText = findViewById(R.id.speech_result)

        // 저장된 결과 표시
        updateResults()
    }

    private fun updateResults() {
        val prefs = getSharedPreferences("analysis_results", MODE_PRIVATE)

        // Face 결과 업데이트
        val faceScore = prefs.getFloat("face_score", -1f)
        val faceStroke = prefs.getInt("face_stroke", -1)
        faceResultText.text = """
            Score: $faceScore
            Stroke: $faceStroke
            Threshold: ${if (faceStroke == 1) "FAIL" else if (faceStroke == 0) "PASS" else "NOT TESTED"}
        """.trimIndent()

        // Arm 결과 업데이트
        val leftArmScore = prefs.getFloat("left_arm_score", -1f)
        val leftArmStroke = prefs.getInt("left_arm_stroke", -1)
        val rightArmScore = prefs.getFloat("right_arm_score", -1f)
        val rightArmStroke = prefs.getInt("right_arm_stroke", -1)
        armResultText.text = """
            Left Arm Score: $leftArmScore
            Left Arm Stroke: $leftArmStroke
            Right Arm Score: $rightArmScore
            Right Arm Stroke: $rightArmStroke
            Threshold: ${if (leftArmStroke == 1 || rightArmStroke == 1) "FAIL" else if (leftArmStroke == 0 && rightArmStroke == 0) "PASS" else "NOT TESTED"}
        """.trimIndent()

        // Speech 결과 업데이트
        val speechScore = prefs.getFloat("speech_score", -1f)
        val speechStroke = prefs.getInt("speech_stroke", -1)
        speechResultText.text = """
            Score: $speechScore
            Stroke: $speechStroke
            Threshold: ${if (speechStroke == 1) "FAIL" else if (speechStroke == 0) "PASS" else "NOT TESTED"}
        """.trimIndent()
    }


}