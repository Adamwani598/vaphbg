package com.example.network.models

import android.graphics.Bitmap
import com.example.data.BlueprintEngine
import com.example.data.BlueprintEntity
import com.example.data.CostBreakdown
import com.example.data.RoomItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Enumeration of captured media sources that can be analyzed by Gemini Vision AI.
 */
enum class CapturedMediaType(val label: String, val description: String) {
    SKETCH_IMAGE("Hand-Drawn Sketch", "Paper blueprint or whiteboard layout drawing"),
    PHYSICAL_PHOTO("Site / Room Photo", "Single photo of architectural drawing or room site"),
    MULTI_ANGLE_PHOTOS("Multi-Angle Photos", "Multiple captured perspectives of a space or drawing"),
    WALKTHROUGH_VIDEO_FRAMES("Walkthrough Video Frames", "Sequenced video frames captured during room walkthrough"),
    MIXED_MEDIA("Composite Multimodal", "Combination of sketches, photos, and voice notes")
}

/**
 * Telemetry collected during a walkthrough scan (e.g. step counters, elapsed time, compass heading).
 */
data class WalkthroughTelemetry(
    val durationSeconds: Int = 15,
    val estimatedPacedDistanceMeters: Float = 12.5f,
    val detectedRoomsCount: Int = 3,
    val deviceHeadingDegrees: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User-specified generation constraints and parameters.
 */
data class BlueprintGenerationSpecs(
    val targetAreaSqm: Float = 100f,
    val preferredBedrooms: Int = 3,
    val preferredBathrooms: Int = 2,
    val county: String = "Nairobi",
    val roofType: String = "Mabati (Iron Sheets)",
    val finishesQuality: String = "Standard",
    val soilType: String = "Red Soil",
    val extraInstructions: String = ""
)

/**
 * Bounding dimensions for the entire 2D floor plan layout.
 */
data class BuildingDimensions(
    val widthMeters: Double = 12.0,
    val lengthMeters: Double = 10.0,
    val totalFootprintSqm: Double = 120.0,
    val perimeterMeters: Double = 44.0
)

/**
 * Placement specification for a door on a room wall.
 */
data class DoorPlacement(
    val wallOrientation: String = "SOUTH", // NORTH, SOUTH, EAST, WEST
    val offsetMeters: Double = 1.0,
    val widthMeters: Double = 0.9,
    val type: String = "SINGLE_DOOR", // SINGLE_DOOR, DOUBLE_DOOR, SLIDING, POCKET
    val connectsTo: String? = null // Target room name if internal door
)

/**
 * Placement specification for a window on an exterior or courtyard wall.
 */
data class WindowPlacement(
    val wallOrientation: String = "NORTH", // NORTH, SOUTH, EAST, WEST
    val offsetMeters: Double = 1.0,
    val widthMeters: Double = 1.2,
    val heightMeters: Double = 1.5,
    val type: String = "CASEMENT" // CASEMENT, SLIDING, FIXED, LOUVER
)

/**
 * Individual room definition in the 2D floor plan coordinate space.
 */
data class FloorPlanRoom(
    val id: String,
    val name: String,
    val type: String, // LIVING_ROOM, MASTER_BEDROOM, BEDROOM, KITCHEN, BATHROOM, DINING, CORRIDOR, BALCONY, PORCH, STORE, LAUNDRY
    val width: Double, // in meters (X-axis span)
    val height: Double, // in meters (Y-axis span / depth)
    val x: Double, // in meters (top-left X coordinate)
    val y: Double, // in meters (top-left Y coordinate)
    val color: String = "#2196F3",
    val features: List<String> = emptyList(),
    val doors: List<DoorPlacement> = emptyList(),
    val windows: List<WindowPlacement> = emptyList(),
    val notes: String = ""
) {
    val areaSqm: Double get() = width * height
}

/**
 * Bill of quantities / material cost estimate element.
 */
data class MaterialEstimate(
    val name: String,
    val quantity: String,
    val estimatedUnitCostKes: Long,
    val totalCostKes: Long,
    val category: String = "Structural"
)

/**
 * Architectural, zoning, and construction specifications for the layout.
 */
data class FloorPlanSpecifications(
    val estimatedCostMinKes: Long = 2_500_000L,
    val estimatedCostMaxKes: Long = 3_800_000L,
    val recommendedRoofType: String = "Mabati (Iron Sheets)",
    val recommendedSoilType: String = "Red Soil",
    val recommendedFinishes: String = "Standard",
    val zoningComplianceNotes: String = "Meets standard residential setbacks (minimum 1.5m to boundary).",
    val ventilationAndLightingRating: String = "EXCELLENT",
    val materialSummary: List<MaterialEstimate> = emptyList()
)

/**
 * Root structured JSON data representing the complete 2D floor plan layout parsed by Gemini AI.
 */
data class FloorPlanLayoutData(
    val title: String,
    val architecturalStyle: String = "Modern Kenyan Bungalow",
    val totalAreaSqm: Double,
    val buildingDimensions: BuildingDimensions,
    val description: String,
    val confidenceScore: Double = 0.92,
    val mediaType: CapturedMediaType = CapturedMediaType.SKETCH_IMAGE,
    val rooms: List<FloorPlanRoom>,
    val doors: List<DoorPlacement> = emptyList(),
    val windows: List<WindowPlacement> = emptyList(),
    val specifications: FloorPlanSpecifications = FloorPlanSpecifications(),
    val rawJson: String = "",
    val isSuccessful: Boolean = true,
    val errorMessage: String? = null
) {
    /**
     * Converts the structured FloorPlanLayoutData into a persistent Room Database BlueprintEntity.
     */
    fun toBlueprintEntity(county: String = "Nairobi"): BlueprintEntity {
        val roomsArray = JSONArray()
        rooms.forEach { r ->
            val obj = JSONObject()
            obj.put("name", r.name)
            obj.put("width", r.width)
            obj.put("height", r.height)
            obj.put("x", r.x)
            obj.put("y", r.y)
            obj.put("color", r.color)
            roomsArray.put(obj)
        }

        return BlueprintEntity(
            title = title.ifBlank { "Gemini AI Floor Plan" },
            description = "$description\n\nStyle: $architecturalStyle | Confidence: ${(confidenceScore * 100).toInt()}%",
            roomsJson = roomsArray.toString(),
            totalAreaSqm = totalAreaSqm.toFloat(),
            estimatedCostMinKes = specifications.estimatedCostMinKes.toDouble(),
            estimatedCostMaxKes = specifications.estimatedCostMaxKes.toDouble(),
            plotSize = "50x100 ft",
            county = county,
            status = "Draft",
            notes = "Structured 2D layout generated via Gemini Vision AI (${mediaType.label}). Includes ${rooms.size} spatial zones."
        )
    }

    /**
     * Converts rooms list into RoomItem list used by canvas view.
     */
    fun toRoomItems(): List<RoomItem> {
        return rooms.map { r ->
            RoomItem(
                name = r.name,
                width = r.width.toFloat(),
                height = r.height.toFloat(),
                x = r.x.toFloat(),
                y = r.y.toFloat(),
                color = r.color
            )
        }
    }

    /**
     * Converts specifications to CostBreakdown with standard construction scheduling.
     */
    fun toCostBreakdown(): CostBreakdown {
        val area = totalAreaSqm.toFloat().coerceAtLeast(20f)
        return BlueprintEngine.calculateCostBreakdown(
            areaSqm = area,
            finishesQuality = specifications.recommendedFinishes,
            roofType = specifications.recommendedRoofType,
            soilType = specifications.recommendedSoilType
        )
    }
}

/**
 * Encapsulates an individual captured media asset (photo, sketch, or video walkthrough) in the user's gallery.
 */
data class CapturedMediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val mediaType: CapturedMediaType,
    val bitmap: Bitmap? = null,
    val secondaryBitmaps: List<Bitmap> = emptyList(),
    val videoFilePath: String? = null,
    val videoDurationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedRoomCount: Int = 1,
    val notes: String = "",
    val qualityScore: Int = 94,
    val resolutionLabel: String = "1920×1080 FHD",
    val telemetry: WalkthroughTelemetry? = null
) {
    val totalFramesCount: Int
        get() = (if (bitmap != null) 1 else 0) + secondaryBitmaps.size

    val allBitmaps: List<Bitmap>
        get() = listOfNotNull(bitmap) + secondaryBitmaps
}


