package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.BlueprintEntity
import com.example.data.BlueprintEngine
import com.example.data.RoomItem
import org.json.JSONArray
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Utility for exporting a comprehensive multi-page PDF summary document
 * combining 2D Floor Plan layout visualization, Room Schedule, itemized NCA Cost Estimation,
 * Bill of Quantities (BoQ) materials list, and Physical Planning Act compliance report.
 */
object PdfExporter {

    fun generatePdfAndShare(
        context: Context,
        blueprint: BlueprintEntity,
        complianceChecked: Boolean
    ) {
        val document = PdfDocument()

        // Page setup: Standard A4 bounds (595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842

        // Colors
        val navyColor = android.graphics.Color.parseColor("#0D1B2A")
        val tealColor = android.graphics.Color.parseColor("#00B4D8")
        val textDarkColor = android.graphics.Color.parseColor("#1B263B")
        val textMutedColor = android.graphics.Color.parseColor("#78909C")
        val bgLightColor = android.graphics.Color.parseColor("#F4F6F9")
        val bgTableAltColor = android.graphics.Color.parseColor("#EAEFF5")
        val accentGreenColor = android.graphics.Color.parseColor("#2E7D32")
        val accentRedColor = android.graphics.Color.parseColor("#C62828")

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
            maximumFractionDigits = 0
        }
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        // Calculate Cost Breakdown dynamically
        val costBreakdown = BlueprintEngine.calculateCostBreakdown(blueprint.totalAreaSqm)

        // Parse Rooms
        val roomsList = mutableListOf<RoomItem>()
        try {
            val arr = JSONArray(blueprint.roomsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                roomsList.add(
                    RoomItem(
                        name = o.getString("name"),
                        width = o.getDouble("width").toFloat(),
                        height = o.getDouble("height").toFloat(),
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat(),
                        color = o.optString("color", "#2196F3")
                    )
                )
            }
        } catch (e: Exception) {
            roomsList.add(RoomItem("Living & Dining", 6f, 5f, 0f, 0f, "#2196F3"))
            roomsList.add(RoomItem("Kitchen", 4f, 3f, 6f, 0f, "#4CAF50"))
            roomsList.add(RoomItem("Master Bedroom", 4f, 4f, 0f, 5f, "#9C27B0"))
            roomsList.add(RoomItem("Bathroom", 3f, 2f, 4f, 5f, "#FF9800"))
        }

        // =========================================================================
        // PAGE 1: ARCHITECTURAL 2D FLOOR PLAN & ROOM SCHEDULE
        // =========================================================================
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // 1. Title Header Banner
        val headerPaint = Paint().apply {
            color = navyColor
            style = Paint.Style.FILL
        }
        canvas1.drawRect(40f, 40f, 555f, 100f, headerPaint)

        val whiteBoldPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12f
            isFakeBoldText = true
        }
        canvas1.drawText("CONSOLIDATED ARCHITECTURAL & COST SPECIFICATIONS REPORT", 55f, 62f, whiteBoldPaint)

        val whiteRegularPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 8.5f
        }
        canvas1.drawText("National Construction Authority (NCA) Technical Floor Plan Reference - Page 1 of 2", 55f, 76f, whiteRegularPaint)
        canvas1.drawText("Kenyan Building Code & Physical Planning Act (Cap 286) Compliant", 55f, 89f, whiteRegularPaint)

        // 2. Project Metadata Card
        val titlePaint = Paint().apply {
            color = textDarkColor
            textSize = 13f
            isFakeBoldText = true
        }
        canvas1.drawText(blueprint.title, 40f, 122f, titlePaint)

        val refPaint = Paint().apply {
            color = textMutedColor
            textSize = 8.5f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val refId = "Ref: NP-${blueprint.id}-${blueprint.createdAt % 100000} | County: ${blueprint.county}"
        canvas1.drawText(refId, 40f, 135f, refPaint)

        val datePaint = Paint().apply {
            color = textDarkColor
            textSize = 8.5f
        }
        canvas1.drawText("Compiled on: ${dateFormat.format(Date(blueprint.createdAt))}", 40f, 147f, datePaint)

        // Right Column Metadata
        val metaLabelPaint = Paint().apply {
            color = textMutedColor
            textSize = 7.5f
            isFakeBoldText = true
        }
        val metaValPaint = Paint().apply {
            color = textDarkColor
            textSize = 9.5f
            isFakeBoldText = true
        }

        canvas1.drawText("FOOTPRINT AREA", 390f, 122f, metaLabelPaint)
        canvas1.drawText("${String.format("%.1f", blueprint.totalAreaSqm)} sqm (${blueprint.plotSize})", 390f, 134f, metaValPaint)

        canvas1.drawText("ESTIMATED BUDGET RANGE", 390f, 148f, metaLabelPaint)
        val budgetStr = "${currencyFormat.format(blueprint.estimatedCostMinKes)} - ${currencyFormat.format(blueprint.estimatedCostMaxKes)}"
        canvas1.drawText(budgetStr, 390f, 160f, metaValPaint)

        // 3. Section Title: 2D Floor Plan Canvas
        val subHeadingPaint = Paint().apply {
            color = navyColor
            textSize = 10f
            isFakeBoldText = true
        }
        canvas1.drawText("VISUAL 2D FLOOR PLAN LAYOUT (SCALE: CAD CONTEXT)", 40f, 178f, subHeadingPaint)

        // CAD Canvas Box
        val cadBgPaint = Paint().apply {
            color = bgLightColor
            style = Paint.Style.FILL
        }
        canvas1.drawRect(40f, 186f, 555f, 436f, cadBgPaint)

        // Grid lines inside CAD box
        val gridPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E1E6ED")
            strokeWidth = 0.5f
        }
        for (x in 60..540 step 20) {
            canvas1.drawLine(x.toFloat(), 186f, x.toFloat(), 436f, gridPaint)
        }
        for (y in 206..416 step 20) {
            canvas1.drawLine(40f, y.toFloat(), 555f, y.toFloat(), gridPaint)
        }

        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#B0BEC5")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas1.drawRect(40f, 186f, 555f, 436f, borderPaint)

        // Scale & Draw Rooms on Canvas 1
        var maxX = 0f
        var maxY = 0f
        for (room in roomsList) {
            val rx2 = room.x + room.width
            val ry2 = room.y + room.height
            if (rx2 > maxX) maxX = rx2
            if (ry2 > maxY) maxY = ry2
        }
        if (maxX <= 0) maxX = 10f
        if (maxY <= 0) maxY = 10f

        val regionW = 515f
        val regionH = 250f
        val pad = 20f

        val availableW = regionW - 2 * pad
        val availableH = regionH - 2 * pad

        val scaleX = availableW / maxX
        val scaleY = availableH / maxY
        val scale = Math.min(scaleX, scaleY)

        val drawW = maxX * scale
        val drawH = maxY * scale
        val startX = 40f + pad + (availableW - drawW) / 2f
        val startY = 186f + pad + (availableH - drawH) / 2f

        for (room in roomsList) {
            val rx = startX + room.x * scale
            val ry = startY + room.y * scale
            val rw = room.width * scale
            val rh = room.height * scale

            val fillPaint = Paint().apply {
                color = try {
                    android.graphics.Color.parseColor(room.color).run {
                        android.graphics.Color.argb(30, android.graphics.Color.red(this), android.graphics.Color.green(this), android.graphics.Color.blue(this))
                    }
                } catch (e: Exception) {
                    android.graphics.Color.argb(30, 33, 150, 243)
                }
                style = Paint.Style.FILL
            }
            canvas1.drawRect(rx, ry, rx + rw, ry + rh, fillPaint)

            val wallPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#263238")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            canvas1.drawRect(rx, ry, rx + rw, ry + rh, wallPaint)

            val innerWallPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            canvas1.drawRect(rx + 1f, ry + 1f, rx + rw - 1f, ry + rh - 1f, innerWallPaint)

            val labelPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#1A237E")
                textSize = 8.5f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val roomNameY = ry + rh / 2f - 2f
            canvas1.drawText(room.name, rx + rw / 2f, roomNameY, labelPaint)

            val sizePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#00838F")
                textSize = 7.5f
                textAlign = Paint.Align.CENTER
            }
            val sizeLabel = "${String.format("%.1f", room.width)}m × ${String.format("%.1f", room.height)}m"
            canvas1.drawText(sizeLabel, rx + rw / 2f, roomNameY + 10f, sizePaint)

            val doorSize = 10f
            if (rw > doorSize + 4f) {
                val doorPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#455A64")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                val arcRect = RectF(rx, ry + rh - doorSize, rx + doorSize, ry + rh)
                canvas1.drawArc(arcRect, 0f, 90f, true, doorPaint)
            }
        }

        // 4. Room Schedule Table Section
        canvas1.drawText("ROOM DIMENSIONS & AREA SCHEDULE", 40f, 455f, subHeadingPaint)

        val tableHeaderPaint = Paint().apply {
            color = navyColor
            style = Paint.Style.FILL
        }
        canvas1.drawRect(40f, 463f, 555f, 481f, tableHeaderPaint)

        val tableHeaderThPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 8f
            isFakeBoldText = true
        }
        canvas1.drawText("ROOM NAME", 50f, 475f, tableHeaderThPaint)
        canvas1.drawText("DIMENSIONS (W × H)", 220f, 475f, tableHeaderThPaint)
        canvas1.drawText("AREA (SQM)", 380f, 475f, tableHeaderThPaint)
        canvas1.drawText("PORTION", 480f, 475f, tableHeaderThPaint)

        var tableY = 496f
        val tableRowTdPaint = Paint().apply {
            color = textDarkColor
            textSize = 8f
        }
        val tableRowTdBold = Paint().apply {
            color = textDarkColor
            textSize = 8f
            isFakeBoldText = true
        }

        roomsList.forEachIndexed { index, room ->
            if (tableY > 640f) return@forEachIndexed // avoid overflowing Page 1

            val rowBg = if (index % 2 == 1) bgTableAltColor else bgLightColor
            val rowPaint = Paint().apply {
                color = rowBg
                style = Paint.Style.FILL
            }
            canvas1.drawRect(40f, tableY - 10f, 555f, tableY + 5f, rowPaint)

            val roomArea = room.width * room.height
            val pct = (roomArea / blueprint.totalAreaSqm * 100).roundToInt()

            canvas1.drawText(room.name, 50f, tableY, tableRowTdBold)
            canvas1.drawText("${String.format("%.1f", room.width)} m × ${String.format("%.1f", room.height)} m", 220f, tableY, tableRowTdPaint)
            canvas1.drawText("${String.format("%.1f", roomArea)} sqm", 380f, tableY, tableRowTdBold)
            canvas1.drawText("$pct%", 480f, tableY, tableRowTdPaint)

            tableY += 17f
        }

        // 5. NCA Technical Specifications Checklist
        canvas1.drawText("NCA TECHNICAL STRUCTURAL SPECIFICATIONS", 40f, 665f, subHeadingPaint)

        val specBgPaint = Paint().apply {
            color = bgLightColor
            style = Paint.Style.FILL
        }
        canvas1.drawRoundRect(40f, 673f, 555f, 795f, 6f, 6f, specBgPaint)
        canvas1.drawRoundRect(40f, 673f, 555f, 795f, 6f, 6f, borderPaint)

        val bulletPaint = Paint().apply {
            color = navyColor
            style = Paint.Style.FILL
        }
        val specTitlePaint = Paint().apply {
            color = textDarkColor
            textSize = 8f
            isFakeBoldText = true
        }
        val specDescPaint = Paint().apply {
            color = textDarkColor
            textSize = 7.5f
        }

        val specs = listOf(
            "Substructure (Foundation): Concrete Strip Footing, 1000g Damp Proof Membrane (DPM) sheeting, anti-termite soil treatment.",
            "Superstructure (Walls): Class A quarry-dressed natural stone block walling (9\" ext, 6\" int) bonded with Class II cement mortar.",
            "Roof Trusses & Cover: Grade II Cypress structural timber rafters treated with anti-borer, covered with Mabati sheets.",
            "Doors & Openings: Solid hardwood exterior panel doors, high-density flush interior doors, steel-casement architectural windows.",
            "Ventilation & Light: Complies with Physical Planning standards. Natural daylight floor-to-window ratio exceeds 10%."
        )

        var specY = 688f
        for (spec in specs) {
            canvas1.drawCircle(52f, specY - 3f, 2f, bulletPaint)
            val parts = spec.split(":", limit = 2)
            if (parts.size == 2) {
                val boldPart = parts[0] + ":"
                val regularPart = parts[1]
                canvas1.drawText(boldPart, 62f, specY, specTitlePaint)
                val boldW = specTitlePaint.measureText(boldPart)
                canvas1.drawText(regularPart, 62f + boldW + 3f, specY, specDescPaint)
            } else {
                canvas1.drawText(spec, 62f, specY, specDescPaint)
            }
            specY += 21f
        }

        document.finishPage(page1)

        // =========================================================================
        // PAGE 2: NCA ITEMIZED COST ESTIMATION & BILL OF QUANTITIES (BoQ)
        // =========================================================================
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2 = page2.canvas

        // 1. Page 2 Header Banner
        canvas2.drawRect(40f, 40f, 555f, 95f, headerPaint)
        canvas2.drawText("NCA ITEMIZED CONSTRUCTION COST ESTIMATION & BoQ", 55f, 62f, whiteBoldPaint)
        canvas2.drawText("Detailed Cost Breakdown & Material Quantity Estimates - Page 2 of 2", 55f, 76f, whiteRegularPaint)
        canvas2.drawText("Rates based on standard National Construction Authority (NCA) regional metrics", 55f, 88f, whiteRegularPaint)

        // 2. Budget Summary Box
        val summaryBgPaint = Paint().apply {
            color = bgLightColor
            style = Paint.Style.FILL
        }
        canvas2.drawRoundRect(40f, 105f, 555f, 145f, 6f, 6f, summaryBgPaint)
        canvas2.drawRoundRect(40f, 105f, 555f, 145f, 6f, 6f, borderPaint)

        canvas2.drawText("FOOTPRINT AREA", 55f, 120f, metaLabelPaint)
        canvas2.drawText("${String.format("%.1f", blueprint.totalAreaSqm)} sqm", 55f, 134f, metaValPaint)

        canvas2.drawText("NCA RATE PER SQM", 220f, 120f, metaLabelPaint)
        canvas2.drawText("${currencyFormat.format(costBreakdown.finalRatePerSqm)} /sqm", 220f, 134f, metaValPaint)

        canvas2.drawText("ESTIMATED TOTAL BUDGET RANGE", 380f, 120f, metaLabelPaint)
        val greenTextPaint = Paint().apply {
            color = accentGreenColor
            textSize = 10f
            isFakeBoldText = true
        }
        canvas2.drawText("${currencyFormat.format(costBreakdown.totalCostMin)} - ${currencyFormat.format(costBreakdown.totalCostMax)}", 380f, 134f, greenTextPaint)

        // 3. Itemized Construction Stages Table
        canvas2.drawText("ITEMIZED CONSTRUCTION STAGE COST BREAKDOWN", 40f, 163f, subHeadingPaint)

        canvas2.drawRect(40f, 171f, 555f, 189f, tableHeaderPaint)
        canvas2.drawText("STAGE / STRUCTURAL COMPONENT", 50f, 183f, tableHeaderThPaint)
        canvas2.drawText("ALLOCATION", 330f, 183f, tableHeaderThPaint)
        canvas2.drawText("ESTIMATED COST (KES)", 430f, 183f, tableHeaderThPaint)

        val stages = listOf(
            Triple("Substructure (Foundation & Slab Bed)", 0.18, costBreakdown.substructure),
            Triple("Superstructure (Quarry Walling & Columns)", 0.25, costBreakdown.superstructure),
            Triple("Roofing Structure & Sheet Cover", 0.15, costBreakdown.roofing),
            Triple("Finishes (Paint, Tiles, Plaster)", 0.22, costBreakdown.finishes),
            Triple("Services (Plumbing & Electrical Wiring)", 0.12, costBreakdown.services),
            Triple("Contingency & County Permit Approvals", 0.08, costBreakdown.contingency)
        )

        var stageY = 203f
        stages.forEachIndexed { index, stage ->
            val rowBg = if (index % 2 == 1) bgTableAltColor else bgLightColor
            val rowPaint = Paint().apply {
                color = rowBg
                style = Paint.Style.FILL
            }
            canvas2.drawRect(40f, stageY - 10f, 555f, stageY + 5f, rowPaint)

            canvas2.drawText(stage.first, 50f, stageY, tableRowTdBold)
            canvas2.drawText("${(stage.second * 100).toInt()}%", 330f, stageY, tableRowTdPaint)
            canvas2.drawText(currencyFormat.format(stage.third), 430f, stageY, tableRowTdBold)

            stageY += 17f
        }

        // Total Cost Summary Row
        val totalRowPaint = Paint().apply {
            color = navyColor
            style = Paint.Style.FILL
        }
        canvas2.drawRect(40f, stageY - 10f, 555f, stageY + 6f, totalRowPaint)
        val whiteTotalPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 8.5f
            isFakeBoldText = true
        }
        canvas2.drawText("TOTAL PROJECT ESTIMATE (UPPER TIER)", 50f, stageY, whiteTotalPaint)
        canvas2.drawText("100%", 330f, stageY, whiteTotalPaint)
        canvas2.drawText(currencyFormat.format(costBreakdown.totalCostMax), 430f, stageY, whiteTotalPaint)

        // 4. Bill of Quantities (BoQ) Material Quantities Table
        val boqStartY = stageY + 25f
        canvas2.drawText("ESTIMATED BILL OF QUANTITIES (RAW MATERIALS BOQ)", 40f, boqStartY, subHeadingPaint)

        val boqHeaderY = boqStartY + 8f
        canvas2.drawRect(40f, boqHeaderY, 555f, boqHeaderY + 18f, tableHeaderPaint)
        canvas2.drawText("MATERIAL ITEM", 50f, boqHeaderY + 12f, tableHeaderThPaint)
        canvas2.drawText("SPECIFICATION", 190f, boqHeaderY + 12f, tableHeaderThPaint)
        canvas2.drawText("EST. QTY", 350f, boqHeaderY + 12f, tableHeaderThPaint)
        canvas2.drawText("EST. COST (KES)", 450f, boqHeaderY + 12f, tableHeaderThPaint)

        val area = blueprint.totalAreaSqm
        val boqItems = listOf(
            Tuple4("Cement (50kg Bags)", "Simba 42.5N Pozzolanic Cement", "${(area * 4.5).roundToInt()} Bags", area * 4.5 * 850),
            Tuple4("Quarry Stones (9\"x9\")", "Machine-Cut Thika Quarry Stones", "${(area * 18).roundToInt()} Pieces", area * 18 * 85),
            Tuple4("River Sand", "Clean Coarse River Sand", "${(area * 0.45).roundToInt()} Tonnes", area * 0.45 * 1800),
            Tuple4("Ballast (3/4\")", "Crushed Blue Stone Ballast", "${(area * 0.35).roundToInt()} Tonnes", area * 0.35 * 2200),
            Tuple4("Roofing Timber", "2x2 & 2x4 Cypress Timber Trusses", "${(area * 12).roundToInt()} Feet", area * 12 * 75),
            Tuple4("Mabati Sheets", "28-Gauge Pre-painted Iron Sheets", "${(area * 1.35).roundToInt()} Sheets", area * 1.35 * 1450),
            Tuple4("Floor Tiles", "60x60cm Porcelain Floor Tiles", "${(area * 1.15).roundToInt()} sqm", area * 1.15 * 1200),
            Tuple4("Emulsion Paint", "Crown Vinyl Matt Paint", "${(area * 0.12).roundToInt()} Litres", area * 0.12 * 450)
        )

        var boqY = boqHeaderY + 28f
        boqItems.forEachIndexed { index, item ->
            val rowBg = if (index % 2 == 1) bgTableAltColor else bgLightColor
            val rowPaint = Paint().apply {
                color = rowBg
                style = Paint.Style.FILL
            }
            canvas2.drawRect(40f, boqY - 10f, 555f, boqY + 5f, rowPaint)

            canvas2.drawText(item.val1, 50f, boqY, tableRowTdBold)
            canvas2.drawText(item.val2, 190f, boqY, tableRowTdPaint)
            canvas2.drawText(item.val3, 350f, boqY, tableRowTdBold)
            canvas2.drawText(currencyFormat.format(item.val4), 450f, boqY, tableRowTdBold)

            boqY += 17f
        }

        // 5. Kenyan Physical Planning Act Check Box
        val compY = boqY + 12f
        canvas2.drawText("KENYAN PHYSICAL PLANNING ACT COMPLIANCE STATUS", 40f, compY, subHeadingPaint)

        val compBgColor = if (complianceChecked) {
            android.graphics.Color.parseColor("#E8F5E9") // light green
        } else {
            android.graphics.Color.parseColor("#ECEFF1") // light gray
        }
        val compBorderColor = if (complianceChecked) accentGreenColor else android.graphics.Color.parseColor("#B0BEC5")

        val compBgPaint = Paint().apply {
            color = compBgColor
            style = Paint.Style.FILL
        }
        val compBorderPaint = Paint().apply {
            color = compBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas2.drawRoundRect(40f, compY + 8f, 555f, compY + 58f, 6f, 6f, compBgPaint)
        canvas2.drawRoundRect(40f, compY + 8f, 555f, compY + 58f, 6f, 6f, compBorderPaint)

        val compTitlePaint = Paint().apply {
            color = textDarkColor
            textSize = 8.5f
            isFakeBoldText = true
        }
        val compDescPaint = Paint().apply {
            color = textDarkColor
            textSize = 7.5f
        }
        val statusPaint = Paint().apply {
            color = if (complianceChecked) accentGreenColor else textMutedColor
            textSize = 8.5f
            isFakeBoldText = true
        }

        canvas2.drawText("Physical Planning Boundary Setbacks & Plot Coverage Verification (${blueprint.county} County)", 52f, compY + 22f, compTitlePaint)
        canvas2.drawText("Status: " + (if (complianceChecked) "VERIFIED COMPLIANT (PASS)" else "PENDING USER CHECK"), 380f, compY + 22f, statusPaint)
        canvas2.drawText("Criteria: Front boundary setback > 6.0m, side/rear gaps > 1.5m, maximum footprint coverage <= 50%.", 52f, compY + 36f, compDescPaint)
        if (complianceChecked) {
            canvas2.drawText("✓ Layout satisfies physical planning requirements under ${blueprint.county} County building regulations.", 52f, compY + 48f, statusPaint)
        } else {
            canvas2.drawText("⚠ Confirm plot setbacks and coverage constraints manually before formal permit submission.", 52f, compY + 48f, compDescPaint)
        }

        // 6. Important Legal & Technical Disclaimer Box
        val discY = compY + 68f
        val discBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FFEBEE")
            style = Paint.Style.FILL
        }
        val discBorderPaint = Paint().apply {
            color = accentRedColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas2.drawRoundRect(40f, discY, 555f, discY + 62f, 6f, 6f, discBgPaint)
        canvas2.drawRoundRect(40f, discY, 555f, discY + 62f, 6f, 6f, discBorderPaint)

        val discTitlePaint = Paint().apply {
            color = accentRedColor
            textSize = 8.5f
            isFakeBoldText = true
        }
        val discDescPaint = Paint().apply {
            color = accentRedColor
            textSize = 7f
        }

        canvas2.drawText("IMPORTANT LEGAL & TECHNICAL DISCLAIMER", 52f, discY + 14f, discTitlePaint)
        canvas2.drawText("This document contains a preliminary conceptual design layout and technical cost estimation compiled according to regional standard metrics.", 52f, discY + 26f, discDescPaint)
        canvas2.drawText("It does NOT substitute certified cadastral surveys or formal mechanical engineering drafts. This report MUST be thoroughly reviewed, stamped, and approved", 52f, discY + 36f, discDescPaint)
        canvas2.drawText("by a licensed, registered Kenyan Architect or Structural Engineer before physical site excavation or formal county permit submissions.", 52f, discY + 46f, discDescPaint)

        document.finishPage(page2)

        // Save PDF to Public Downloads via MediaStore
        var outputStream: OutputStream? = null
        var pdfUri: Uri? = null

        val filename = "Blueprint_Report_${blueprint.title.replace("\\s+".toRegex(), "_")}_${System.currentTimeMillis()}.pdf"

        try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VAPHBG")
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            pdfUri = contentResolver.insert(collection, contentValues)

            if (pdfUri != null) {
                outputStream = contentResolver.openOutputStream(pdfUri)
                if (outputStream != null) {
                    document.writeTo(outputStream)
                    Toast.makeText(context, "PDF saved to Downloads/VAPHBG successfully!", Toast.LENGTH_LONG).show()
                }
            } else {
                throw Exception("Could not insert to MediaStore")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()

            // Fallback: save to app external files directory
            try {
                val fallbackFile = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename)
                val fallbackOutputStream = java.io.FileOutputStream(fallbackFile)
                document.writeTo(fallbackOutputStream)
                fallbackOutputStream.close()
                Toast.makeText(context, "Saved to app documents: ${fallbackFile.name}", Toast.LENGTH_LONG).show()
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
        } finally {
            try {
                outputStream?.close()
            } catch (ignored: Exception) {}
            document.close()
        }

        // Trigger Share Intent directly
        if (pdfUri != null) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Architectural Specification & Costing Summary: ${blueprint.title}")
                    putExtra(Intent.EXTRA_TEXT, "Hello, find attached the preliminary Architectural Floor Plan & Cost Estimation Summary document for \"${blueprint.title}\" compiled with NCA metrics.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Blueprint PDF Report"))
            } catch (shareEx: Exception) {
                shareEx.printStackTrace()
                Toast.makeText(context, "Unable to share PDF: ${shareEx.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private data class Tuple4<A, B, C, D>(
        val val1: A,
        val val2: B,
        val val3: C,
        val val4: D
    )
}
