// ApiService.kt
import com.example.fast2.api.StrokeAnalysisResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// StrokeApiService.kt
interface StrokeApiService {
    @Multipart
    @POST("face")  // /analyze 제거
    suspend fun analyzeFace(
        @Part image: MultipartBody.Part
    ): Response<StrokeAnalysisResponse>

    @Multipart
    @POST("arm")
    suspend fun analyzeArm(
        @Part csv: MultipartBody.Part
    ): Response<StrokeAnalysisResponse>

    @Multipart
    @POST("speech")
    suspend fun analyzeSpeech(
        @Part audio: MultipartBody.Part
    ): Response<StrokeAnalysisResponse>
}