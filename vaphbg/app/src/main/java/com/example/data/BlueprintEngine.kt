package com.example.data

import kotlin.math.roundToInt

object BlueprintEngine {

    // NCA construction rate categories in Kenya (KES per sqm)
    const val RATE_BUDGET = 35000.0
    const val RATE_STANDARD = 48000.0
    const val RATE_PREMIUM = 65000.0

    /**
     * Generates a fully-aligned, clean 2D floor plan layout based on inputs.
     * Uses deterministic block partitioning so rooms are adjacent and do not overlap.
     */
    fun generateFloorPlan(
        numBedrooms: Int,
        numBathrooms: Int,
        finishesQuality: String = "Standard",
        targetAreaSqm: Float = 100f
    ): List<RoomItem> {
        val rooms = mutableListOf<RoomItem>()
        
        // Let's determine total dimensions of the house block
        // Standard aspect ratio near square or slightly rectangular (e.g. 1.2:1)
        val totalWidth = when {
            targetAreaSqm < 60f -> 8.0f
            targetAreaSqm < 100f -> 10.0f
            targetAreaSqm < 140f -> 12.0f
            else -> 14.0f
        }
        val totalHeight = (targetAreaSqm / totalWidth).roundTo(1)

        // Let's define the grid based on number of bedrooms
        when (numBedrooms) {
            1 -> {
                // 1 Bedroom Layout (e.g. 8m x 7m = 56 sqm)
                // Left side: Living Room & Kitchen
                // Right side: Master Bedroom & Bathroom
                val wLeft = totalWidth * 0.55f
                val wRight = totalWidth - wLeft
                
                val hTop = totalHeight * 0.6f
                val hBottom = totalHeight - hTop

                // Living Room (Top Left)
                rooms.add(RoomItem("Living & Dining", wLeft, hTop, 0f, 0f, "#2196F3"))
                // Kitchen (Bottom Left)
                rooms.add(RoomItem("Kitchen", wLeft, hBottom, 0f, hTop, "#4CAF50"))
                // Bedroom (Top Right)
                rooms.add(RoomItem("Master Bedroom", wRight, hTop, wLeft, 0f, "#9C27B0"))
                // Bathroom (Bottom Right)
                rooms.add(RoomItem("Bathroom", wRight, hBottom, wLeft, hTop, "#FF9800"))
            }
            2 -> {
                // 2 Bedroom Layout (e.g. 10m x 9m = 90 sqm)
                // Columns: Left (Living/Kitchen), Right (Bedrooms & Bathrooms)
                val wLeft = totalWidth * 0.5f
                val wRight = totalWidth - wLeft

                val hLeftTop = totalHeight * 0.65f
                val hLeftBottom = totalHeight - hLeftTop

                val hRightBed1 = totalHeight * 0.4f
                val hRightBed2 = totalHeight * 0.4f
                val hRightBath = totalHeight - hRightBed1 - hRightBed2

                // Living & Dining (Top Left)
                rooms.add(RoomItem("Living & Dining", wLeft, hLeftTop, 0f, 0f, "#2196F3"))
                // Kitchen (Bottom Left)
                rooms.add(RoomItem("Kitchen", wLeft * 0.7f, hLeftBottom, 0f, hLeftTop, "#4CAF50"))
                // Store/Pantry (Bottom Left Inside)
                rooms.add(RoomItem("Store & Utility", wLeft * 0.3f, hLeftBottom, wLeft * 0.7f, hLeftTop, "#607D8B"))

                // Master Bedroom (Top Right)
                rooms.add(RoomItem("Master Bedroom", wRight, hRightBed1, wLeft, 0f, "#9C27B0"))
                // Bedroom 2 (Middle Right)
                rooms.add(RoomItem("Bedroom 2", wRight, hRightBed2, wLeft, hRightBed1, "#E91E63"))
                // Shared Bathroom (Bottom Right)
                rooms.add(RoomItem("Bathroom", wRight, hRightBath, wLeft, hRightBed1 + hRightBed2, "#FF9800"))
            }
            3 -> {
                // 3 Bedroom Layout (e.g. 12m x 10m = 120 sqm)
                // Left Column: Living Room (Top), Kitchen (Bottom)
                // Middle Column: Dining/Corridor (Top), Bedroom 2 (Bottom), Bathroom 1
                // Right Column: Master Suite (Top), Bedroom 3 (Bottom)
                val wLeft = totalWidth * 0.35f
                val wMiddle = totalWidth * 0.3f
                val wRight = totalWidth - wLeft - wMiddle

                val hTop = totalHeight * 0.55f
                val hBottom = totalHeight - hTop

                // Left Column
                rooms.add(RoomItem("Living Room", wLeft, hTop, 0f, 0f, "#2196F3"))
                rooms.add(RoomItem("Kitchen", wLeft, hBottom, 0f, hTop, "#4CAF50"))

                // Middle Column
                rooms.add(RoomItem("Dining Area", wMiddle, hTop * 0.5f, wLeft, 0f, "#00BCD4"))
                rooms.add(RoomItem("Common Bath", wMiddle, hTop * 0.5f, wLeft, hTop * 0.5f, "#FF9800"))
                rooms.add(RoomItem("Bedroom 2", wMiddle, hBottom, wLeft, hTop, "#E91E63"))

                // Right Column
                rooms.add(RoomItem("Master Bedroom", wRight, hTop * 0.7f, wLeft + wMiddle, 0f, "#9C27B0"))
                rooms.add(RoomItem("Master Bath", wRight, hTop * 0.3f, wLeft + wMiddle, hTop * 0.7f, "#FF5722"))
                rooms.add(RoomItem("Bedroom 3", wRight, hBottom, wLeft + wMiddle, hTop, "#3F51B5"))
            }
            else -> {
                // 4+ Bedrooms (Generates a multi-room spacious layout)
                val wCol = totalWidth / 3.0f
                val hRow = totalHeight / 3.0f

                rooms.add(RoomItem("Living Room", wCol * 1.5f, hRow * 1.5f, 0f, 0f, "#2196F3"))
                rooms.add(RoomItem("Dining Room", wCol * 1.5f, hRow * 0.7f, 0f, hRow * 1.5f, "#00BCD4"))
                rooms.add(RoomItem("Kitchen", wCol * 1.5f, hRow * 0.8f, 0f, hRow * 2.2f, "#4CAF50"))

                rooms.add(RoomItem("Master Bedroom", wCol * 1.5f, hRow * 1.2f, wCol * 1.5f, 0f, "#9C27B0"))
                rooms.add(RoomItem("Master Bath", wCol * 0.6f, hRow * 0.6f, wCol * 1.5f, hRow * 1.2f, "#FF5722"))
                rooms.add(RoomItem("Guest Bath", wCol * 0.9f, hRow * 0.6f, wCol * 2.1f, hRow * 1.2f, "#FF9800"))

                rooms.add(RoomItem("Bedroom 2", wCol, hRow * 1.2f, wCol * 1.5f, hRow * 1.8f, "#E91E63"))
                rooms.add(RoomItem("Bedroom 3", wCol, hRow * 1.2f, wCol * 2.5f, hRow * 1.8f, "#3F51B5"))
                
                val remainingHeight = totalHeight - (hRow * 1.8f) - (hRow * 1.2f)
                if (remainingHeight > 1f) {
                    rooms.add(RoomItem("Verandah", totalWidth - wCol * 1.5f, remainingHeight, wCol * 1.5f, hRow * 1.8f + hRow * 1.2f, "#795548"))
                }
            }
        }
        
        return rooms
    }

    /**
     * Calculates construction costs for Kenyan NCA rates, with itemized breakdowns.
     */
    fun calculateCostBreakdown(
        areaSqm: Float,
        finishesQuality: String = "Standard",
        roofType: String = "Mabati (Iron Sheets)",
        soilType: String = "Red Soil"
    ): CostBreakdown {
        val baseRate = when (finishesQuality.lowercase()) {
            "budget" -> RATE_BUDGET
            "premium" -> RATE_PREMIUM
            else -> RATE_STANDARD
        }

        // Apply multipliers based on roof and soil factors
        val roofMultiplier = when {
            roofType.contains("Concrete", true) -> 1.15
            roofType.contains("Tiles", true) -> 1.08
            else -> 1.00 // standard Mabati
        }

        val soilMultiplier = when {
            soilType.contains("Clay", true) || soilType.contains("Black Cotton", true) -> 1.18 // requires expensive strip or raft foundation
            soilType.contains("Rock", true) -> 1.08 // requires excavation breaking
            else -> 1.00 // standard red soil
        }

        val finalRate = baseRate * roofMultiplier * soilMultiplier
        val totalCost = areaSqm * finalRate
        
        // Itemized percentage breakdown based on Kenyan construction metrics
        val substructurePercent = 0.18 // Foundation (higher for clay soil)
        val superstructurePercent = 0.25 // Walls, columns, beams
        val roofingPercent = 0.15 // Roof trusses and sheets/tiles
        val finishesPercent = 0.22 // Plaster, paint, tiles, sanitaryware
        val servicesPercent = 0.12 // Electrical, plumbing
        val contingencyPercent = 0.08 // Contingency/NCA approvals

        return CostBreakdown(
            baseRatePerSqm = baseRate,
            finalRatePerSqm = finalRate.roundTo(2),
            totalCostMin = (totalCost * 0.93).roundTo(2),
            totalCostMax = (totalCost * 1.07).roundTo(2),
            substructure = (totalCost * substructurePercent).roundTo(2),
            superstructure = (totalCost * superstructurePercent).roundTo(2),
            roofing = (totalCost * roofingPercent).roundTo(2),
            finishes = (totalCost * finishesPercent).roundTo(2),
            services = (totalCost * servicesPercent).roundTo(2),
            contingency = (totalCost * contingencyPercent).roundTo(2)
        )
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (this * multiplier).roundToInt() / multiplier
    }

    private fun Float.roundTo(decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10f }
        return (this * multiplier).roundToInt() / multiplier
    }
}

data class CostBreakdown(
    val baseRatePerSqm: Double,
    val finalRatePerSqm: Double,
    val totalCostMin: Double,
    val totalCostMax: Double,
    val substructure: Double,
    val superstructure: Double,
    val roofing: Double,
    val finishes: Double,
    val services: Double,
    val contingency: Double
)
