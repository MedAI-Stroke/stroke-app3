package com.example.fast2

import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")  // Camera API 사용을 위한 경고 무시
class DiagnosisF2Activity : AppCompatActivity(), SurfaceHolder.Callback {
    private var camera: Camera? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_f2)

        surfaceView = findViewById(R.id.camera_preview)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)

        // 3초 후 다음 액티비티로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            releaseCamera()
            startActivity(Intent(this, DiagnosisA1Activity::class.java))
        }, 3000)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
            camera?.setDisplayOrientation(90)
            camera?.setPreviewDisplay(holder)
            camera?.startPreview()
        } catch (e: Exception) {
            Toast.makeText(this, "카메라를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // 필요한 경우 프리뷰 크기 조정
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    private fun releaseCamera() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }
}