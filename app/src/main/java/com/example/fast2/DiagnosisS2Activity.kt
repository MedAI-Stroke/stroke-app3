package com.example.fast2

import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fast2.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class DiagnosisS2Activity : AppCompatActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var outputFile: File
    private val isTestMode = true  // 테스트 모드 플래그

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_s2)

        if (isTestMode) {
            // 테스트 모드: 3초 후 더미 데이터 전송
            Handler(Looper.getMainLooper()).postDelayed({
                sendTestAudioData()
            }, 3000)
        } else {
            // 실제 모드: 녹음 시작
            setupRecorder()
            startRecording()

            // 5초 동안 녹음 후 API 전송
            Handler(Looper.getMainLooper()).postDelayed({
                stopRecording()
                uploadAudioFile()
            }, 5000)
        }
    }

    private fun setupRecorder() {
        outputFile = File(cacheDir, "audio_record.wav")  // WAV 형식으로 변경

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@DiagnosisS2Activity, "녹음 준비 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        try {
            mediaRecorder?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음 시작 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음 종료 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendTestAudioData() {
        try {
            // assets 폴더의 테스트 오디오 파일을 캐시로 복사
            val inputStream = assets.open("test_audio.wav")
            val testFile = File(cacheDir, "test_audio.wav")

            testFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            uploadAudioFile(testFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "테스트 파일 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            navigateToNextScreen()
        }
    }

    private fun uploadAudioFile(file: File = outputFile) {
        lifecycleScope.launch {
            try {
                // 파일을 MultipartBody.Part로 변환
                val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)

                // API 호출
                val response = withContext(Dispatchers.IO) {
                    ApiClient.strokeApiService.analyzeSpeech(audioPart)
                }

                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        // 결과 저장
                        getSharedPreferences("analysis_results", MODE_PRIVATE).edit().apply {
                            putFloat("speech_score", result.result.score)
                            putInt("speech_stroke", result.result.stroke)
                            apply()
                        }

                        // 결과에 따른 Toast 메시지
                        val message = if (result.result.stroke == 1) {
                            "이상 징후가 감지되었습니다"
                        } else {
                            "정상입니다"
                        }
                        Toast.makeText(this@DiagnosisS2Activity, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw Exception("API 오류: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@DiagnosisS2Activity,
                    "음성 분석 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                file.delete()  // 임시 파일 삭제
                navigateToNextScreen()
            }
        }
    }

    private fun navigateToNextScreen() {
        startActivity(Intent(this, AnalysisMainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
        if (::outputFile.isInitialized && outputFile.exists()) {
            outputFile.delete()
        }
    }
}