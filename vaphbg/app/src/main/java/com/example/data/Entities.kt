package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class RoomItem(
    val name: String,
    val width: Float, // in meters
    val height: Float, // in meters
    val x: Float, // relative layout position
    val y: Float, // relative layout position
    val color: String = "#2196F3"
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passcodeHash: String, // simple hashed/stored pin
    val biometricEnabled: Boolean = false,
    val role: String = "Architect & Homeowner",
    val phone: String = "+254 700 000 000",
    val county: String = "Nairobi",
    val preferredCurrency: String = "KES", // KES, USD, EUR
    val preferredUnit: String = "Sqm", // Sqm, Sqft
    val constructionPreset: String = "Mid-Range Finishes",
    val avatarColorHex: String = "#00B4D8",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blueprints")
data class BlueprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val roomsJson: String, // Serialized List<RoomItem>
    val totalAreaSqm: Float,
    val estimatedCostMinKes: Double,
    val estimatedCostMaxKes: Double,
    val plotSize: String = "50x100 ft",
    val county: String = "Nairobi",
    val status: String = "Draft", // Draft, Approved, Exported
    val notes: String = "",
    val category: String = "Residential", // Residential, Commercial, Bungalow, Maisonette, Apartment, Custom
    val tags: String = "" // Comma-separated custom tags (e.g. "Modern, 3-Bed, Budget")
) {
    fun getTagList(): List<String> {
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

data class CountyPreset(
    val name: String,
    val maxCoveragePct: Int,
    val minSetbackMeters: Float,
    val maxFloors: Int,
    val approvalFeeKes: Double,
    val sewageRequirement: String,
    val description: String
)

object CountyZoningPresets {
    val presets = listOf(
        CountyPreset(
            name = "Nairobi",
            maxCoveragePct = 50,
            minSetbackMeters = 3.0f,
            maxFloors = 4,
            approvalFeeKes = 15000.0,
            sewageRequirement = "Main Sewer / Bio-digester",
            description = "Nairobi County structural rules under NCC Physical Planning standards. Requires mandatory structural engineer sign-off if over 2 storeys."
        ),
        CountyPreset(
            name = "Kiambu",
            maxCoveragePct = 60,
            minSetbackMeters = 2.5f,
            maxFloors = 3,
            approvalFeeKes = 12000.0,
            sewageRequirement = "Septic Tank / Bio-septic",
            description = "Kiambu County guidelines. Generous coverage allowance. High focus on agricultural boundary buffers."
        ),
        CountyPreset(
            name = "Mombasa",
            maxCoveragePct = 65,
            minSetbackMeters = 2.0f,
            maxFloors = 5,
            approvalFeeKes = 18000.0,
            sewageRequirement = "Soak Pit / Bio-digester",
            description = "Coast region guidelines. High humidity and high water table restrictions apply. Special sea-breeze setback regulations."
        ),
        CountyPreset(
            name = "Machakos",
            maxCoveragePct = 60,
            minSetbackMeters = 3.0f,
            maxFloors = 3,
            approvalFeeKes = 10000.0,
            sewageRequirement = "Septic Tank / Bio-septic",
            description = "Eastern region guidelines. Semi-arid zone concrete curing regulations apply. Soil compaction reports required."
        ),
        CountyPreset(
            name = "Nakuru",
            maxCoveragePct = 55,
            minSetbackMeters = 3.0f,
            maxFloors = 3,
            approvalFeeKes = 11000.0,
            sewageRequirement = "Septic Tank / Local Sewer",
            description = "Rift Valley guidelines. Volcanic ash soil structural reinforcement required for foundations."
        ),
        CountyPreset(
            name = "Kisumu",
            maxCoveragePct = 55,
            minSetbackMeters = 3.0f,
            maxFloors = 4,
            approvalFeeKes = 13000.0,
            sewageRequirement = "Septic Tank / Sewer Line",
            description = "Lake region guidelines. Cotton soil standard foundation slab thickness requirement applies."
        )
    )

    fun getPreset(name: String): CountyPreset {
        return presets.find { it.name.equals(name, ignoreCase = true) } ?: presets[0]
    }
}
