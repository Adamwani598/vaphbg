package com.example.network.service

import android.graphics.Bitmap
import com.example.network.models.BlueprintGenerationSpecs
import com.example.network.models.CapturedMediaType
import com.example.network.models.FloorPlanLayoutData
import com.example.network.models.WalkthroughTelemetry

/**
 * Service layer abstraction for analyzing visual and multimodal media (sketches, site photos,
 * video scanner frames, voice notes) via Gemini API to generate structured 2D architectural floor plans.
 */
interface GeminiFloorPlanAnalysisService {

    /**
     * Analyzes a single captured floor plan image or hand-drawn sketch photo.
     */
    suspend fun analyzeFloorPlanImage(
        bitmap: Bitmap,
        mediaType: CapturedMediaType = CapturedMediaType.SKETCH_IMAGE,
        userPrompt: String = "",
        contextCounty: String = "Nairobi",
        specs: BlueprintGenerationSpecs? = null
    ): Result<FloorPlanLayoutData>

    /**
     * Analyzes multiple photos from different angles or multi-page sketches.
     */
    suspend fun analyzeMultiAnglePhotos(
        bitmaps: List<Bitmap>,
        userPrompt: String = "",
        contextCounty: String = "Nairobi",
        specs: BlueprintGenerationSpecs? = null
    ): Result<FloorPlanLayoutData>

    /**
     * Analyzes a sequence of video frames extracted from a walkthrough room scan,
     * incorporating sensor telemetry (estimated paced distance, elapsed seconds, room transitions).
     */
    suspend fun analyzeWalkthroughVideoFrames(
        frames: List<Bitmap>,
        telemetry: WalkthroughTelemetry? = null,
        userPrompt: String = "",
        contextCounty: String = "Nairobi",
        specs: BlueprintGenerationSpecs? = null
    ): Result<FloorPlanLayoutData>

    /**
     * Directly parses raw prompt and optional reference media to structured 2D layout JSON.
     */
    suspend fun analyzeCustomPrompt(
        prompt: String,
        mediaBitmaps: List<Bitmap> = emptyList(),
        mediaType: CapturedMediaType = CapturedMediaType.MIXED_MEDIA
    ): Result<FloorPlanLayoutData>
}
