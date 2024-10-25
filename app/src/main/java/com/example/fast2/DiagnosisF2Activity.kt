package com.example.fast2

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fast2.api.ApiClient
import com.example.fast2.api.StrokeAnalysisResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class DiagnosisF2Activity : AppCompatActivity(), SurfaceHolder.Callback {
    private var camera: Camera? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private var capturedImage: Bitmap? = null
    private var isCameraCaptured = false
    private var pictureFile: File? = null
    private lateinit var loadingProgress: ProgressBar
    private var isTestMode = true  // 테스트 모드 플래그


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.diagnosis_f2)

        surfaceView = findViewById(R.id.camera_preview)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
        loadingProgress = findViewById(R.id.loading_progress)

        val captureButton: ImageButton = findViewById(R.id.capture_button)
        captureButton.setOnClickListener {
            if (isTestMode) {
                // 테스트 모드: 더미 이미지 사용
                sendTestImage()
            } else {
                // 실제 모드: 카메라 촬영
                takePicture()
            }
        }
    }

    private fun sendTestImage() {
        try {
            // assets 폴더에서 테스트 이미지 로드
            val inputStream = assets.open("test_face.jpg")
            val file = File(cacheDir, "test_face.jpg")

            // 파일로 저장
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            pictureFile = file
            uploadImage()

        } catch (e: Exception) {
            Toast.makeText(this, "테스트 이미지 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
            camera?.setDisplayOrientation(90)

            val parameters = camera?.parameters
            val sizes = parameters?.supportedPreviewSizes
            val optimalSize = getOptimalPreviewSize(sizes, surfaceView.width, surfaceView.height)
            parameters?.setPreviewSize(optimalSize?.width ?: 640, optimalSize?.height ?: 480)
            camera?.parameters = parameters

            camera?.setPreviewDisplay(holder)
            camera?.startPreview()
        } catch (e: Exception) {
            Toast.makeText(this, "카메라를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (surfaceHolder.surface == null) return

        try {
            camera?.stopPreview()
        } catch (e: Exception) {
            // 프리뷰가 존재하지 않는 경우 무시
        }

        try {
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    private fun takePicture() {
        camera?.takePicture(null, null, Camera.PictureCallback { data, _ ->
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                val matrix = Matrix()
                matrix.postRotate(270f)
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height,
                    matrix, true
                )

                pictureFile = File(cacheDir, "captured_face.jpg")
                FileOutputStream(pictureFile).use { fos ->
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }

                capturedImage = rotatedBitmap
                isCameraCaptured = true

                uploadImage()

            } catch (e: Exception) {
                Toast.makeText(this, "사진 촬영에 실패했습니다.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        })
    }

    private fun showLoading() {
        loadingProgress.visibility = android.view.View.VISIBLE
    }

    private fun hideLoading() {
        loadingProgress.visibility = android.view.View.GONE
    }

    private fun releaseCamera() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun uploadImage() {
        pictureFile?.let { file ->
            showLoading()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData(
                        "image",
                        file.name,
                        requestFile
                    )

                    val response = ApiClient.strokeApiService.analyzeFace(imagePart)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            response.body()?.let { result ->
                                // 결과 저장
                                saveAnalysisResult("face", result)

                                // 다음 화면으로 이동
                                navigateToNextScreen()

                                // 선택적: 결과에 따른 Toast 메시지 표시
                                val message = if (result.result.stroke == 1) {
                                    "이상 징후가 감지되었습니다"
                                } else {
                                    "정상입니다"
                                }
                                Toast.makeText(this@DiagnosisF2Activity, message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(
                                this@DiagnosisF2Activity,
                                "분석 실패: ${response.errorBody()?.string()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@DiagnosisF2Activity,
                            "네트워크 오류: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                    }
                }
            }
        }
    }

    private fun navigateToNextScreen() {
        releaseCamera()
        startActivity(Intent(this, DiagnosisA1Activity::class.java))
        finish()
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, targetWidth: Int, targetHeight: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = targetWidth.toDouble() / targetHeight

        if (sizes.isNullOrEmpty()) return null

        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        return optimalSize ?: sizes[0]
    }

    private fun saveAnalysisResult(type: String, response: StrokeAnalysisResponse) {
        getSharedPreferences("analysis_results", MODE_PRIVATE).edit().apply {
            // API 응답에서 받은 score와 stroke 값을 저장
            putFloat("${type}_score", response.result.score)
            putInt("${type}_stroke", response.result.stroke)
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCamera()
        pictureFile?.delete()
    }
}