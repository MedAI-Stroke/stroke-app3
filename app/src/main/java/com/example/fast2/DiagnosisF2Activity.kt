package com.example.fast2

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity

class DiagnosisF2Activity : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 100
    private var capturedImage: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_f2)

        // 3초 후에 카메라 자동 실행
        Handler().postDelayed({
            capturePhoto()
        }, 3000)
    }

    private fun capturePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            capturedImage = data.extras?.get("data") as Bitmap
            Handler().postDelayed({
                startActivity(Intent(this, DiagnosisA1Activity::class.java))
            }, 3000) // 3초 후 DiagnosisA1Activity로 전환
        }
    }
}
