package com.example.fast2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
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
    private val isTestMode = true // 테스트 모드
    private lateinit var mediaPlayer: MediaPlayer

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 2048
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_s2)

        // MediaPlayer 초기화 및 mp3 파일 설정
        mediaPlayer = MediaPlayer.create(this, R.raw.s)
        mediaPlayer.setOnCompletionListener {
            // s.mp3 파일 재생이 완료되면 녹음 시작
            if (checkPermission()) {
                startWavRecording()
                // 5초 후 녹음 중지
                Handler(Looper.getMainLooper()).postDelayed({
                    stopRecording()
                    uploadAudioFile()
                }, 5000)
            }
        }

        // mp3 파일 재생
        mediaPlayer.start()
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

        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(fos, totalDataSize)

            FileInputStream(tempDataFile).use { fis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                }
            }
        }

        tempDataFile.delete()
    }

    private fun writeWavHeader(outputStream: FileOutputStream, totalDataSize: Int) {
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + totalDataSize)
        buffer.put("WAVE".toByteArray())

        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())

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
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
