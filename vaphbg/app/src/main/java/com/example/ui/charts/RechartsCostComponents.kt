package com.example.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CostBreakdown
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BlueprintCyan
import java.text.NumberFormat
import kotlin.math.*

// Visual Chart Stage Palette matching Recharts modern vibrant aesthetic
val ChartPink = Color(0xFFEC4899)
val ChartCyan = Color(0xFF00B4D8)
val ChartPurple = Color(0xFF8B5CF6)
val ChartEmerald = Color(0xFF10B981)
val ChartAmber = Color(0xFFF59E0B)
val ChartSlate = Color(0xFF64748B)
val ChartIndigo = Color(0xFF6366F1)
val ChartCoral = Color(0xFFFF6B6B)

data class ChartDataPoint(
    val label: String,
    val shortLabel: String,
    val value: Double,
    val percentage: Float,
    val color: Color,
    val benchmarkPct: Float = 0f,
    val description: String = ""
)

/**
 * Recharts-inspired Interactive Donut Chart with dynamic slice selection,
 * center statistics, and interactive legend pills.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RechartsDonutChart(
    breakdown: CostBreakdown,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf(0) } // 0: Materials vs Labor, 1: NCA Structural Stages
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val avgTotalCost = (breakdown.totalCostMin + breakdown.totalCostMax) / 2.0

    val dataPoints = remember(breakdown, chartMode) {
        if (chartMode == 0) {
            listOf(
                ChartDataPoint("Direct Materials", "Materials", avgTotalCost * 0.55, 55f, ChartCyan, 55f, "Cement, stone blocks, steel rebars, timber & mabati sheets"),
                ChartDataPoint("Artisan & Skilled Labor", "Labor", avgTotalCost * 0.33, 33f, ChartEmerald, 30f, "Masons, carpenters, plumbers, electricians & casual site workers"),
                ChartDataPoint("Equipment & Permits", "Services", avgTotalCost * 0.12, 12f, ChartAmber, 15f, "Excavator hire, scaffolding, NCA compliance fees & transport")
            )
        } else {
            listOf(
                ChartDataPoint("Substructure", "Substr", breakdown.substructure, 18f, ChartPink, 18f, "Excavations, strip footing foundation, hardcore and concrete floor bed"),
                ChartDataPoint("Superstructure", "Superstr", breakdown.superstructure, 25f, ChartCyan, 25f, "Quarry stone walling, mortar, lintels, concrete ring beams & columns"),
                ChartDataPoint("Roofing & Trusses", "Roofing", breakdown.roofing, 15f, ChartPurple, 15f, "Treated timber trusses, wall plates and gauge 28 box profile mabati"),
                ChartDataPoint("Finishes & Decor", "Finishes", breakdown.finishes, 22f, ChartEmerald, 22f, "Internal/external plaster, floor tiles, sanitary fixtures & premium paint"),
                ChartDataPoint("Services & Utilities", "Services", breakdown.services, 12f, ChartAmber, 12f, "Piping, water tank plumbing, conduit wiring & electrical distribution"),
                ChartDataPoint("Contingency & NCA", "Contin.", breakdown.contingency, 8f, ChartSlate, 8f, "County municipal permits, NCA registration & unexpected variations")
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_donut_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with title and Recharts-style toggle filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cost Distribution (Donut)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = chartMode == 0,
                        onClick = {
                            chartMode = 0
                            selectedIndex = null
                        },
                        label = { Text("Resource Split", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("filter_chip_resource_split")
                    )
                    FilterChip(
                        selected = chartMode == 1,
                        onClick = {
                            chartMode = 1
                            selectedIndex = null
                        },
                        label = { Text("NCA Stages", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("filter_chip_stages_donut")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Donut Canvas
            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                val dist = sqrt(dx * dx + dy * dy)
                                val outerR = size.width / 2f
                                val innerR = outerR - 40.dp.toPx()

                                if (dist in innerR..outerR) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    var startAngle = -90f
                                    var clicked: Int? = null
                                    for (i in dataPoints.indices) {
                                        val sweep = (dataPoints[i].percentage / 100f) * 360f
                                        val nStart = (startAngle % 360 + 360) % 360
                                        val nEnd = nStart + sweep
                                        val testA = if (angle < nStart && nEnd > 360) angle + 360 else angle
                                        if (testA in nStart..nEnd) {
                                            clicked = i
                                            break
                                        }
                                        startAngle += sweep
                                    }
                                    selectedIndex = if (selectedIndex == clicked) null else clicked
                                } else {
                                    selectedIndex = null
                                }
                            }
                        }
                ) {
                    val strokeW = 34.dp.toPx()
                    val diameter = size.minDimension - strokeW - 14.dp.toPx()
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    var curAngle = -90f
                    dataPoints.forEachIndexed { index, pt ->
                        val sweep = (pt.percentage / 100f) * 360f
                        val isSel = selectedIndex == index
                        val activeStroke = if (isSel) strokeW + 8.dp.toPx() else strokeW
                        val alpha = if (selectedIndex == null || isSel) 1f else 0.35f

                        drawArc(
                            color = pt.color.copy(alpha = alpha),
                            startAngle = curAngle,
                            sweepAngle = (sweep - 2.5f).coerceAtLeast(0.1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = activeStroke, cap = StrokeCap.Round)
                        )
                        curAngle += sweep
                    }
                }

                // Center Dynamic Metric
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (selectedIndex != null && selectedIndex!! in dataPoints.indices) {
                        val pt = dataPoints[selectedIndex!!]
                        Text(
                            text = pt.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "${pt.percentage.toInt()}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = pt.color,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatKshShort(pt.value, currencyFormat),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "Estimated Budget",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatKshShort(avgTotalCost, currencyFormat),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap segment to inspect",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recharts-style Interactive Legend Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                maxItemsInEachRow = 3
            ) {
                dataPoints.forEachIndexed { index, pt ->
                    val isSel = selectedIndex == index
                    Surface(
                        color = if (isSel) pt.color.copy(alpha = 0.18f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clickable {
                                selectedIndex = if (selectedIndex == index) null else index
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(pt.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${pt.shortLabel} (${pt.percentage.toInt()}%)",
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) pt.color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-style Cartesian Bar Chart with interactive bars, gridlines,
 * value labels, and a floating detail tooltip on tap.
 */
@Composable
fun RechartsBarChart(
    breakdown: CostBreakdown,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val stages = remember(breakdown) {
        listOf(
            ChartDataPoint("Substructure", "Found.", breakdown.substructure, 18f, ChartPink, 18f, "Excavation, footings & concrete ground slab"),
            ChartDataPoint("Superstructure", "Walls", breakdown.superstructure, 25f, ChartCyan, 25f, "Stone block walls, mortar, lintels & ring beams"),
            ChartDataPoint("Roofing", "Roof", breakdown.roofing, 15f, ChartPurple, 15f, "Treated timber trusses & IT5 box mabati sheets"),
            ChartDataPoint("Finishes", "Finish", breakdown.finishes, 22f, ChartEmerald, 22f, "Plaster, porcelain floor tiles & premium paints"),
            ChartDataPoint("Services", "Util.", breakdown.services, 12f, ChartAmber, 12f, "Plumbing pipes, drainage, cabling & distribution"),
            ChartDataPoint("Contingency", "Cont.", breakdown.contingency, 8f, ChartSlate, 8f, "County fees, NCA certificates & site variances")
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_bar_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stage Breakdown (Bar Chart)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Interactive Tooltips",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recharts Cartesian Canvas Bar Chart
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(stages) {
                            detectTapGestures { tapOffset ->
                                val leftPadding = 36.dp.toPx()
                                val rightPadding = 12.dp.toPx()
                                val bottomPadding = 28.dp.toPx()
                                val topPadding = 16.dp.toPx()
                                val chartW = size.width - leftPadding - rightPadding
                                val slotW = chartW / stages.size

                                if (tapOffset.x >= leftPadding && tapOffset.x <= size.width - rightPadding &&
                                    tapOffset.y >= topPadding && tapOffset.y <= size.height - bottomPadding
                                ) {
                                    val idx = ((tapOffset.x - leftPadding) / slotW).toInt().coerceIn(0, stages.size - 1)
                                    selectedBarIndex = if (selectedBarIndex == idx) null else idx
                                } else {
                                    selectedBarIndex = null
                                }
                            }
                        }
                ) {
                    val leftPad = 36.dp.toPx()
                    val rightPad = 12.dp.toPx()
                    val bottomPad = 28.dp.toPx()
                    val topPad = 16.dp.toPx()
                    val plotW = size.width - leftPad - rightPad
                    val plotH = size.height - topPad - bottomPad

                    val maxPercentage = 30f // scale 0% to 30%

                    // 1. Draw Cartesian Horizontal Grid Lines (0%, 10%, 20%, 30%)
                    val gridSteps = listOf(0f, 10f, 20f, 30f)
                    gridSteps.forEach { stepPct ->
                        val y = topPad + plotH * (1f - (stepPct / maxPercentage))
                        drawLine(
                            color = outlineVariantColor.copy(alpha = 0.25f),
                            start = Offset(leftPad, y),
                            end = Offset(size.width - rightPad, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        // Label for grid
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${stepPct.toInt()}%",
                            topLeft = Offset(2.dp.toPx(), y - 7.dp.toPx()),
                            style = TextStyle(
                                color = onSurfaceColor.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    // 2. Draw Bars with gradients & rounded caps
                    val slotW = plotW / stages.size
                    val barW = (slotW * 0.55f).coerceAtMost(32.dp.toPx())

                    stages.forEachIndexed { index, stage ->
                        val barCenterX = leftPad + (index * slotW) + (slotW / 2f)
                        val barLeft = barCenterX - (barW / 2f)
                        val barH = (stage.percentage / maxPercentage) * plotH
                        val barTop = topPad + plotH - barH
                        val isSelected = selectedBarIndex == index

                        val barAlpha = if (selectedBarIndex == null || isSelected) 1f else 0.4f
                        val barColor = stage.color.copy(alpha = barAlpha)

                        // Bar background container / track
                        drawRoundRect(
                            color = outlineVariantColor.copy(alpha = 0.08f),
                            topLeft = Offset(barLeft, topPad),
                            size = Size(barW, plotH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Active gradient bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    stage.color.copy(alpha = barAlpha),
                                    stage.color.copy(alpha = barAlpha * 0.65f)
                                ),
                                startY = barTop,
                                endY = topPad + plotH
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        if (isSelected) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(barLeft - 2.dp.toPx(), barTop - 2.dp.toPx()),
                                size = Size(barW + 4.dp.toPx(), barH + 4.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Percentage on top of bar
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${stage.percentage.toInt()}%",
                            topLeft = Offset(barCenterX - 8.dp.toPx(), barTop - 14.dp.toPx()),
                            style = TextStyle(
                                color = if (isSelected) stage.color else onSurfaceColor.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )

                        // X-axis label
                        drawText(
                            textMeasurer = textMeasurer,
                            text = stage.shortLabel,
                            topLeft = Offset(barCenterX - 14.dp.toPx(), size.height - bottomPad + 6.dp.toPx()),
                            style = TextStyle(
                                color = if (isSelected) stage.color else onSurfaceColor.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Interactive Tooltip Card on tap
            selectedBarIndex?.let { idx ->
                val stage = stages[idx]
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = stage.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(stage.color.copy(alpha = 0.6f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(stage.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stage.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatKshShort(stage.value, currencyFormat),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stage.description,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-style Cumulative Project Spend S-Curve (AreaChart)
 * Plots the standard construction project cash flow trajectory over 5 phases.
 */
@Composable
fun RechartsProjectSCurveChart(
    breakdown: CostBreakdown,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedNode by remember { mutableStateOf<Int?>(null) }
    val avgTotal = (breakdown.totalCostMin + breakdown.totalCostMax) / 2.0

    data class MilestoneNode(
        val phase: String,
        val shortName: String,
        val cumulativePct: Float,
        val weekSpan: String,
        val focus: String
    ) {
        val cumulativeCost: Double get() = avgTotal * (cumulativePct / 100.0)
    }

    val milestones = remember {
        listOf(
            MilestoneNode("Groundbreaking & Slab", "Found.", 18f, "Wk 1-4", "Excavation, footing & concrete floor"),
            MilestoneNode("Superstructure Walling", "Walls", 43f, "Wk 5-9", "Quarry stones, lintels & ring beams"),
            MilestoneNode("Roofing & Water Tight", "Roof", 58f, "Wk 10-13", "Timber trusses & mabati roofing"),
            MilestoneNode("Finishes & Plumbing", "Finish", 88f, "Wk 14-19", "Plaster, floor tiles, wiring & pipes"),
            MilestoneNode("NCA Final Handover", "Done", 100f, "Wk 20-22", "Fixtures, painting & occupancy permit")
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_scurve_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = BlueprintCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cash Flow S-Curve (Area Chart)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = BlueprintCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Cumulative Outlay",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BlueprintCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(milestones) {
                            detectTapGestures { tapOffset ->
                                val leftPad = 38.dp.toPx()
                                val rightPad = 18.dp.toPx()
                                val bottomPad = 26.dp.toPx()
                                val topPad = 16.dp.toPx()
                                val plotW = size.width - leftPad - rightPad
                                val stepW = plotW / (milestones.size - 1)

                                var closestIdx: Int? = null
                                var minDistance = Float.MAX_VALUE

                                milestones.forEachIndexed { index, _ ->
                                    val x = leftPad + (index * stepW)
                                    val dist = abs(tapOffset.x - x)
                                    if (dist < minDistance && dist < 30.dp.toPx()) {
                                        minDistance = dist
                                        closestIdx = index
                                    }
                                }
                                selectedNode = if (selectedNode == closestIdx) null else closestIdx
                            }
                        }
                ) {
                    val leftPad = 38.dp.toPx()
                    val rightPad = 18.dp.toPx()
                    val bottomPad = 26.dp.toPx()
                    val topPad = 16.dp.toPx()
                    val plotW = size.width - leftPad - rightPad
                    val plotH = size.height - topPad - bottomPad

                    // 1. Grid Lines (0%, 25%, 50%, 75%, 100%)
                    val ySteps = listOf(0f, 25f, 50f, 75f, 100f)
                    ySteps.forEach { step ->
                        val y = topPad + plotH * (1f - (step / 100f))
                        drawLine(
                            color = outlineVariantColor.copy(alpha = 0.2f),
                            start = Offset(leftPad, y),
                            end = Offset(size.width - rightPad, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${step.toInt()}%",
                            topLeft = Offset(2.dp.toPx(), y - 7.dp.toPx()),
                            style = TextStyle(
                                color = onSurfaceColor.copy(alpha = 0.5f),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    // 2. Build Smooth Bezier Path for S-Curve
                    val stepW = plotW / (milestones.size - 1)
                    val points = milestones.mapIndexed { index, m ->
                        val x = leftPad + (index * stepW)
                        val y = topPad + plotH * (1f - (m.cumulativePct / 100f))
                        Offset(x, y)
                    }

                    val linePath = Path()
                    val areaPath = Path()

                    linePath.moveTo(points.first().x, points.first().y)
                    areaPath.moveTo(points.first().x, topPad + plotH)
                    areaPath.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                        linePath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        areaPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                    }

                    areaPath.lineTo(points.last().x, topPad + plotH)
                    areaPath.close()

                    // Fill Gradient Area below S-Curve
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BlueprintCyan.copy(alpha = 0.35f),
                                BlueprintCyan.copy(alpha = 0.02f)
                            ),
                            startY = topPad,
                            endY = topPad + plotH
                        )
                    )

                    // Stroke S-Curve Line
                    drawPath(
                        path = linePath,
                        color = BlueprintCyan,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 3. Draw Milestone Node Dots
                    points.forEachIndexed { index, pt ->
                        val isSel = selectedNode == index
                        val m = milestones[index]

                        // Outer ring if selected
                        if (isSel) {
                            drawCircle(
                                color = BlueprintCyan.copy(alpha = 0.35f),
                                radius = 12.dp.toPx(),
                                center = pt
                            )
                        }

                        drawCircle(
                            color = if (isSel) Color.White else BlueprintCyan,
                            radius = if (isSel) 6.dp.toPx() else 4.5.dp.toPx(),
                            center = pt
                        )

                        drawCircle(
                            color = Color(0xFF0D1B2A),
                            radius = if (isSel) 3.5.dp.toPx() else 2.dp.toPx(),
                            center = pt
                        )

                        // X-axis Phase labels
                        drawText(
                            textMeasurer = textMeasurer,
                            text = m.shortName,
                            topLeft = Offset(pt.x - 12.dp.toPx(), size.height - bottomPad + 6.dp.toPx()),
                            style = TextStyle(
                                color = if (isSel) BlueprintCyan else onSurfaceColor.copy(alpha = 0.7f),
                                fontSize = 8.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Interactive Milestone Callout
            selectedNode?.let { idx ->
                val m = milestones[idx]
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = BlueprintCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(BlueprintCyan.copy(alpha = 0.6f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BlueprintCyan,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${m.cumulativePct.toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0D1B2A)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = m.phase,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = m.weekSpan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueprintCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = m.focus,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Req: ${formatKshShort(m.cumulativeCost, currencyFormat)}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-style Polar Radar Chart comparing Current Design breakdown
 * against Kenyan NCA National Average Baseline profile.
 */
@Composable
fun RechartsRadarChart(
    breakdown: CostBreakdown,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    data class RadarAxis(val name: String, val currentPct: Float, val benchmarkPct: Float)

    val axes = remember(breakdown) {
        listOf(
            RadarAxis("Substr.", 18f, 18f),
            RadarAxis("Superstr.", 25f, 25f),
            RadarAxis("Roofing", 15f, 16f),
            RadarAxis("Finishes", 22f, 20f),
            RadarAxis("Services", 12f, 13f),
            RadarAxis("Contin.", 8f, 8f)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_radar_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = ChartEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Benchmark Radar (Polar Chart)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(ChartEmerald, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Current", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF94A3B8), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NCA Avg", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radar Polar Canvas
            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (size.minDimension / 2f) - 34.dp.toPx()
                    val numAxes = axes.size
                    val angleStep = (2f * Math.PI.toFloat()) / numAxes
                    val maxVal = 30f // scale 0-30%

                    // 1. Draw Concentric Web Rings (25%, 50%, 75%, 100%)
                    val rings = listOf(0.25f, 0.5f, 0.75f, 1f)
                    rings.forEach { fraction ->
                        val r = maxRadius * fraction
                        val ringPath = Path()
                        for (i in 0 until numAxes) {
                            val angle = -Math.PI.toFloat() / 2f + (i * angleStep)
                            val x = center.x + r * cos(angle)
                            val y = center.y + r * sin(angle)
                            if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                        }
                        ringPath.close()
                        drawPath(
                            path = ringPath,
                            color = outlineVariantColor.copy(alpha = 0.25f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 2. Draw Spoke Rays & Labels
                    for (i in 0 until numAxes) {
                        val angle = -Math.PI.toFloat() / 2f + (i * angleStep)
                        val endX = center.x + maxRadius * cos(angle)
                        val endY = center.y + maxRadius * sin(angle)
                        drawLine(
                            color = outlineVariantColor.copy(alpha = 0.35f),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Label
                        val labelRadius = maxRadius + 18.dp.toPx()
                        val labelX = center.x + labelRadius * cos(angle) - 14.dp.toPx()
                        val labelY = center.y + labelRadius * sin(angle) - 6.dp.toPx()
                        drawText(
                            textMeasurer = textMeasurer,
                            text = axes[i].name,
                            topLeft = Offset(labelX, labelY),
                            style = TextStyle(
                                color = onSurfaceColor.copy(alpha = 0.8f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // 3. Draw Benchmark Polygon (Gray)
                    val benchmarkPath = Path()
                    axes.forEachIndexed { i, a ->
                        val angle = -Math.PI.toFloat() / 2f + (i * angleStep)
                        val r = maxRadius * (a.benchmarkPct / maxVal)
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (i == 0) benchmarkPath.moveTo(x, y) else benchmarkPath.lineTo(x, y)
                    }
                    benchmarkPath.close()
                    drawPath(
                        path = benchmarkPath,
                        color = Color(0xFF94A3B8).copy(alpha = 0.15f)
                    )
                    drawPath(
                        path = benchmarkPath,
                        color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                    )

                    // 4. Draw Current Design Polygon (Emerald Green)
                    val currentPath = Path()
                    axes.forEachIndexed { i, a ->
                        val angle = -Math.PI.toFloat() / 2f + (i * angleStep)
                        val r = maxRadius * (a.currentPct / maxVal)
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (i == 0) currentPath.moveTo(x, y) else currentPath.lineTo(x, y)
                    }
                    currentPath.close()
                    drawPath(
                        path = currentPath,
                        color = ChartEmerald.copy(alpha = 0.25f)
                    )
                    drawPath(
                        path = currentPath,
                        color = ChartEmerald,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Current vertices dots
                    axes.forEachIndexed { i, a ->
                        val angle = -Math.PI.toFloat() / 2f + (i * angleStep)
                        val r = maxRadius * (a.currentPct / maxVal)
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        drawCircle(color = ChartEmerald, radius = 3.5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }
    }
}

/**
 * County Regional Construction Cost Index Comparison Bar
 * Compares current project rate per sqm with major Kenyan construction hubs.
 */
@Composable
fun CountyBenchmarkComparisonBar(
    currentRatePerSqm: Double,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    data class CountyCostIndex(val name: String, val avgRateSqm: Double, val tag: String)

    val benchmarkCounties = remember {
        listOf(
            CountyCostIndex("Nairobi", 42000.0, "High Spec"),
            CountyCostIndex("Mombasa", 44500.0, "Coastal / Salt Res."),
            CountyCostIndex("Kiambu", 38500.0, "Suburban"),
            CountyCostIndex("Nakuru", 35000.0, "Rift Valley"),
            CountyCostIndex("Kisumu", 37000.0, "Western"),
            CountyCostIndex("Machakos", 34500.0, "Fast Growing")
        )
    }

    val maxRate = 50000.0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ChartAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Regional County Rate Index",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = AccentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Current: ${formatKshShort(currentRatePerSqm, currencyFormat)}/m²",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            benchmarkCounties.forEach { county ->
                val ratio = (county.avgRateSqm / maxRate).toFloat().coerceIn(0f, 1f)
                val isCurrentBetter = currentRatePerSqm <= county.avgRateSqm

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${county.name} (${county.tag})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${formatKshShort(county.avgRateSqm, currencyFormat)}/m²",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentBetter) MaterialTheme.colorScheme.onSurfaceVariant else ChartPink,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        color = if (county.name == "Nairobi") ChartCyan else ChartPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Recharts Interactive Cost Sensitivity Simulator
 * Allows interactive adjustment of Finishes tier and Contingency buffer with instant feedback.
 */
@Composable
fun RechartsCostSensitivitySimulator(
    baseCostMin: Double,
    baseCostMax: Double,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    var finishTier by remember { mutableStateOf(1) } // 0: Budget/Basic (-15%), 1: Standard (0%), 2: Luxury (+25%)
    var contingencyPct by remember { mutableFloatStateOf(8f) } // 5% to 20%

    val tierMultiplier = when (finishTier) {
        0 -> 0.85
        1 -> 1.00
        else -> 1.25
    }

    val simulatedMin = (baseCostMin * tierMultiplier * (1.0 + (contingencyPct - 8f) / 100.0))
    val simulatedMax = (baseCostMax * tierMultiplier * (1.0 + (contingencyPct - 8f) / 100.0))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cost_sensitivity_simulator_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ChartAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cost Scenario Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = ChartAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Live Sandbox",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChartAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Finishes Quality Selector
            Text(
                text = "Finishes Grade Selection:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Economy (-15%)", "Standard (NCA)", "Luxury (+25%)").forEachIndexed { index, label ->
                    FilterChip(
                        selected = finishTier == index,
                        onClick = { finishTier = index },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contingency Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contingency Buffer Reserve:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${contingencyPct.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChartPink,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = contingencyPct,
                onValueChange = { contingencyPct = it },
                valueRange = 5f..20f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = ChartPink,
                    activeTrackColor = ChartPink
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Result Callout
            Surface(
                color = AccentGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(AccentGreen.copy(alpha = 0.5f))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Simulated Total Project Outlay",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatKshShort(simulatedMin, currencyFormat)} - ${formatKshShort(simulatedMax, currencyFormat)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

fun formatKshShort(amount: Double, formatter: NumberFormat): String {
    return try {
        val formatted = formatter.format(amount)
        formatted.replace("String", "").replace("KES", "Ksh").replace("Ksh", "Ksh ")
    } catch (e: Exception) {
        "Ksh ${amount.toInt()}"
    }
}
