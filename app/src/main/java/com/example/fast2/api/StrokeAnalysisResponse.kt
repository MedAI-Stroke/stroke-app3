// app/src/main/java/com/example/fast2/api/StrokeAnalysisResponse.kt
package com.example.fast2.api

data class StrokeAnalysisResponse(
    val message: String,
    val result: FaceResult,
    val error: String? = null
)

data class FaceResult(
    val stroke: Int,
    val score: Float
)