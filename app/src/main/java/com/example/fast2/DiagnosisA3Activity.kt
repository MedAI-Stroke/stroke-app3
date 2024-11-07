package com.example.fast2

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
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
import java.io.FileWriter
import kotlin.collections.ArrayList
import kotlin.math.sin

class DiagnosisA3Activity : AppCompatActivity(), SensorEventListener {
    private lateinit var countdownText: TextView
    private var countDownTimer: CountDownTimer? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var isMeasuring = false

    private val isTestMode = false // 테스트 모드 비활성화
    private var testStartTime: Long = 0

    private val sensorReadings = ArrayList<SensorReading>()
    private var startTime: Long = 0

    data class SensorReading(
        val samplingTime: Double,
        val accelerationX: Float,
        val accelerationY: Float,
        val accelerationZ: Float,
        val gyroX: Float,
        val gyroY: Float,
        val gyroZ: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_a3)

        countdownText = findViewById(R.id.COUNT_text)

        // a_left.mp3 파일 재생
        val mediaPlayer = MediaPlayer.create(this, R.raw.a_right)
        mediaPlayer.start()

        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
            startCountdownAndMeasurement() // 오디오 재생 후 측정 시작
        }

        if (!isTestMode) {
            setupSensors()
        }
    }

    private fun setupSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (accelerometer == null || gyroscope == null) {
            Toast.makeText(this, "필요한 센서를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun startCountdownAndMeasurement() {
        // 측정 시작
        startTime = System.nanoTime()
        testStartTime = System.nanoTime()
        isMeasuring = true
        sensorReadings.clear()

        if (!isTestMode) {
            val samplingPeriodUs = 50000 // 50ms
            sensorManager.registerListener(this, accelerometer, samplingPeriodUs)
            sensorManager.registerListener(this, gyroscope, samplingPeriodUs)
        }

        // 카운트다운 시작
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                countdownText.text = secondsRemaining.toString()

                if (isTestMode) {
                    generateTestData()
                }
            }

            override fun onFinish() {
                countdownText.text = "0"
                stopMeasurementAndProcessData()
            }
        }.start()
    }

    private fun generateTestData() {
        val currentTime = System.nanoTime()
        val elapsedSeconds = (currentTime - testStartTime) / 1_000_000_000.0

        // 사인파를 사용하여 움직임 시뮬레이션
        val accelerationX = (sin(elapsedSeconds * 2.0) * 2).toFloat()
        val accelerationY = (sin(elapsedSeconds * 3.0) * 1.5).toFloat()
        val accelerationZ = (9.81 + sin(elapsedSeconds) * 0.5).toFloat()

        val gyroX = (sin(elapsedSeconds * 4.0) * 0.5).toFloat()
        val gyroY = (sin(elapsedSeconds * 3.5) * 0.3).toFloat()
        val gyroZ = (sin(elapsedSeconds * 2.5) * 0.4).toFloat()

        sensorReadings.add(
            SensorReading(
                samplingTime = elapsedSeconds,
                accelerationX = accelerationX,
                accelerationY = accelerationY,
                accelerationZ = accelerationZ,
                gyroX = gyroX,
                gyroY = gyroY,
                gyroZ = gyroZ
            )
        )

        android.util.Log.d("DiagnosisA3", "Generated test data point: $elapsedSeconds")
    }

    private fun stopMeasurementAndProcessData() {
        isMeasuring = false
        if (!isTestMode) {
            sensorManager.unregisterListener(this)
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d(
                    "DiagnosisA3",
                    "Creating CSV file with ${sensorReadings.size} readings"
                )
                val csvFile = createCsvFile()
                uploadSensorData(csvFile)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DiagnosisA3Activity,
                        "데이터 처리 중 오류가 발생했습니다: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                e.printStackTrace()
                navigateToNextScreen()
            }
        }
    }

    private fun createCsvFile(): File {
        val file = File(cacheDir, "sensor_data.csv")
        FileWriter(file).use { writer ->
            writer.append("SamplingTime,AccelerationX,AccelerationY,AccelerationZ,GyroX,GyroY,GyroZ\n")

            sensorReadings.forEach { reading ->
                writer.append(
                    "${reading.samplingTime},${reading.accelerationX},${reading.accelerationY}," +
                            "${reading.accelerationZ},${reading.gyroX},${reading.gyroY},${reading.gyroZ}\n"
                )
            }
        }

        android.util.Log.d("DiagnosisA3", "CSV file created: ${file.absolutePath}")
        android.util.Log.d(
            "Diagnosis32",
            "First few lines:\n${file.readLines().take(5).joinToString("\n")}"
        )

        return file
    }

    private suspend fun uploadSensorData(file: File) {
        try {
            android.util.Log.d("DiagnosisA3", "Starting data upload")

            val requestFile = file.asRequestBody("text/csv".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("csv", file.name, requestFile)

            val response = withContext(Dispatchers.IO) {
                ApiClient.strokeApiService.analyzeArm(filePart)
            }

            if (response.isSuccessful) {
                response.body()?.let { result ->
                    android.util.Log.d(
                        "DiagnosisA3",
                        "Upload successful: score=${result.result.score}, stroke=${result.result.stroke}"
                    )

                    // 결과 저장
                    getSharedPreferences("analysis_results", MODE_PRIVATE).edit().apply {
                        putFloat("right_arm_score", result.result.score)
                        putInt("right_arm_stroke", result.result.stroke)
                        apply()
                    }

                }
            } else {
                android.util.Log.e("DiagnosisA3", "API Error: ${response.errorBody()?.string()}")
                throw Exception("API 오류: ${response.errorBody()?.string()}")
            }
        } finally {
            file.delete()
            withContext(Dispatchers.Main) {
                navigateToNextScreen()
            }
        }
    }

    private fun navigateToNextScreen() {
        startActivity(Intent(this, DiagnosisS1Activity::class.java))
        finish()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isMeasuring) return

        val currentTime = System.nanoTime()
        val samplingTime = (currentTime - startTime) / 1_000_000_000.0

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                sensorReadings.add(
                    SensorReading(
                        samplingTime = samplingTime,
                        accelerationX = event.values[0],
                        accelerationY = event.values[1],
                        accelerationZ = event.values[2],
                        gyroX = 0f,
                        gyroY = 0f,
                        gyroZ = 0f
                    )
                )
                android.util.Log.d("DiagnosisA3", "Accelerometer data recorded at $samplingTime")
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (sensorReadings.isNotEmpty()) {
                    val lastReading = sensorReadings.last()
                    if (lastReading.gyroX == 0f && lastReading.gyroY == 0f && lastReading.gyroZ == 0f) {
                        sensorReadings[sensorReadings.lastIndex] = lastReading.copy(
                            gyroX = event.values[0],
                            gyroY = event.values[1],
                            gyroZ = event.values[2]
                        )
                        android.util.Log.d(
                            "DiagnosisA3",
                            "Gyroscope data added to reading at $samplingTime"
                        )
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 구현이 필요 없다면 비워두어도 됩니다
    }
}