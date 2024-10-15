package com.example.fast2

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class DiagnosisA1Activity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var gravitySensor: Sensor? = null
    private var isMeasuring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_a1)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        startMeasuring()

        // 10초 후에 DiagnosisA2Activity로 전환
        Handler().postDelayed({
            stopMeasuring()
            startActivity(Intent(this, DiagnosisA2Activity::class.java))
        }, 10000)
    }

    private fun startMeasuring() {
        isMeasuring = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun stopMeasuring() {
        isMeasuring = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isMeasuring) {
            // 센서 데이터 처리
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 필요에 따라 구현
    }
}
