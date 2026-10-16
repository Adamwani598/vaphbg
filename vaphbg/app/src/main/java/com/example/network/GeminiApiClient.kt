package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.RoomItem
import com.example.network.models.CapturedMediaType
import com.example.network.models.FloorPlanLayoutData
import com.example.network.service.GeminiFloorPlanAnalysisService
import com.example.network.service.GeminiFloorPlanAnalysisServiceImpl
import java.io.ByteArrayOutputStream

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"

    /**
     * Singleton instance of the Gemini Floor Plan Analysis Service.
     */
    val service: GeminiFloorPlanAnalysisService by lazy {
        GeminiFloorPlanAnalysisServiceImpl()
    }

    // Helper to convert Bitmap to Base64
    fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    data class GeminiAnalysisResult(
        val title: String,
        val totalAreaSqm: Float,
        val description: String,
        val rooms: List<RoomItem>,
        val isSuccessful: Boolean = true,
        val errorMessage: String? = null,
        val layoutData: FloorPlanLayoutData? = null
    )

    /**
     * Sends a floor-plan sketch image to Gemini for advanced layout structure & dimension parsing.
     */
    suspend fun analyzeSketchPhoto(
        bitmap: Bitmap,
        extraInstructions: String = "",
        county: String = "Nairobi"
    ): GeminiAnalysisResult {
        val result = service.analyzeFloorPlanImage(
            bitmap = bitmap,
            mediaType = CapturedMediaType.SKETCH_IMAGE,
            userPrompt = extraInstructions,
            contextCounty = county
        )

        return if (result.isSuccess) {
            val layout = result.getOrThrow()
            GeminiAnalysisResult(
                title = layout.title,
                totalAreaSqm = layout.totalAreaSqm.toFloat(),
                description = layout.description,
                rooms = layout.toRoomItems(),
                isSuccessful = layout.isSuccessful,
                errorMessage = layout.errorMessage,
                layoutData = layout
            )
        } else {
            val err = result.exceptionOrNull()
            GeminiAnalysisResult(
                title = "Analysis Error",
                totalAreaSqm = 0f,
                description = err?.localizedMessage ?: "Failed to analyze sketch",
                rooms = emptyList(),
                isSuccessful = false,
                errorMessage = err?.localizedMessage
            )
        }
    }
}

