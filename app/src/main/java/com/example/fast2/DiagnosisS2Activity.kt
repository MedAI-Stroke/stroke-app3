package com.example.fast2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.fast2.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DiagnosisS2Activity : AppCompatActivity() {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private lateinit var outputFile: File
    private val isTestMode = false

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 2048
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_s2)

        if (isTestMode) {
            Handler(Looper.getMainLooper()).postDelayed({
                sendTestAudioData()
            }, 3000)
        } else {
            if (checkPermission()) {
                startWavRecording()
                // 5초 후 녹음 중지
                Handler(Looper.getMainLooper()).postDelayed({
                    stopRecording()
                    uploadAudioFile()
                }, 5000)
            }
        }
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                123
            )
            return false
        }
        return true
    }

    private fun startWavRecording() {
        outputFile = File(cacheDir, "recording.wav")

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBufferSize
        )

        isRecording = true

        lifecycleScope.launch(Dispatchers.IO) {
            writeWavFile()
        }
    }

    private suspend fun writeWavFile() {
        val tempDataFile = File(cacheDir, "temp_audio_data.raw")
        var totalDataSize = 0

        // 먼저 raw 데이터를 임시 파일에 저장
        FileOutputStream(tempDataFile).use { fos ->
            val buffer = ByteArray(BUFFER_SIZE)
            audioRecord?.startRecording()

            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                if (readSize > 0) {
                    fos.write(buffer, 0, readSize)
                    totalDataSize += readSize
                }
            }
        }

        // WAV 파일 생성
        FileOutputStream(outputFile).use { fos ->
            // WAV 헤더 작성
            writeWavHeader(fos, totalDataSize)

            // raw 데이터 복사
            FileInputStream(tempDataFile).use { fis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                }
            }
        }

        // 임시 파일 삭제
        tempDataFile.delete()
    }

    private fun writeWavHeader(outputStream: FileOutputStream, totalDataSize: Int) {
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF 헤더
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + totalDataSize)
        buffer.put("WAVE".toByteArray())

        // fmt 청크
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // 서브청크1크기
        buffer.putShort(1.toShort()) // PCM = 1
        buffer.putShort(1.toShort()) // 모노 = 1
        buffer.putInt(SAMPLE_RATE) // 샘플레이트
        buffer.putInt(SAMPLE_RATE * 2) // 바이트레이트
        buffer.putShort(2.toShort()) // 블록얼라인
        buffer.putShort(16.toShort()) // 비트퍼샘플

        // 데이터 청크
        buffer.put("data".toByteArray())
        buffer.putInt(totalDataSize)

        outputStream.write(buffer.array())
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    private fun sendTestAudioData() {
        try {
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
                val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audio", file.name, requestFile)

                val response = withContext(Dispatchers.IO) {
                    ApiClient.strokeApiService.analyzeSpeech(audioPart)
                }

                if (response.isSuccessful) {
                    response.body()?.let { result ->
                        getSharedPreferences("analysis_results", MODE_PRIVATE).edit().apply {
                            putFloat("speech_score", result.result.score)
                            putInt("speech_stroke", result.result.stroke)
                            apply()
                        }
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
                file.delete()
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
        stopRecording()
        if (::outputFile.isInitialized && outputFile.exists()) {
            outputFile.delete()
        }
    }
}