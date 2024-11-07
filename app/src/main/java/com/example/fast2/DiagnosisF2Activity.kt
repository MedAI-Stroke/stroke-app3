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
    private var isTestMode = false  // 테스트 모드 플래그


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

            // 16:9 비율에 가장 가까운 프리뷰 크기 선택
            val targetRatio = 16.0 / 9.0
            val optimalSize = sizes?.let {
                getOptimalPreviewSize(it, surfaceView.width, surfaceView.height, targetRatio)
            }

            parameters?.setPreviewSize(optimalSize?.width ?: 1280, optimalSize?.height ?: 720)

            // 사진 크기도 설정
            val pictureSizes = parameters?.supportedPictureSizes
            val optimalPictureSize = pictureSizes?.let {
                getOptimalPreviewSize(it, 1280, 720, targetRatio)
            }
            parameters?.setPictureSize(optimalPictureSize?.width ?: 1280,
                optimalPictureSize?.height ?: 720)

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

                // 이미지 크기 축소
                val maxDimension = 1024 // 최대 해상도 지정
                val scaleFactor = Math.min(
                    maxDimension.toFloat() / bitmap.width,
                    maxDimension.toFloat() / bitmap.height
                )

                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scaleFactor).toInt(),
                    (bitmap.height * scaleFactor).toInt(),
                    true
                )

                val rotatedBitmap = Bitmap.createBitmap(
                    resizedBitmap, 0, 0,
                    resizedBitmap.width, resizedBitmap.height,
                    matrix, true
                )

                pictureFile = File(cacheDir, "captured_face.jpg")
                FileOutputStream(pictureFile).use { fos ->
                    // JPEG 품질을 80으로 낮춰서 파일 크기 감소
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
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

    private fun getOptimalPreviewSize(
        sizes: List<Camera.Size>,
        targetWidth: Int,
        targetHeight: Int,
        targetRatio: Double
    ): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue

            val diff = Math.abs(size.height - targetHeight) +
                    Math.abs(size.width - targetWidth)
            if (diff < minDiff) {
                optimalSize = size
                minDiff = diff.toDouble()
            }
        }

        // 비율이 맞는 크기를 찾지 못한 경우, 가장 가까운 크기 반환
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                val diff = Math.abs(size.height - targetHeight) +
                        Math.abs(size.width - targetWidth)
                if (diff < minDiff) {
                    optimalSize = size
                    minDiff = diff.toDouble()
                }
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