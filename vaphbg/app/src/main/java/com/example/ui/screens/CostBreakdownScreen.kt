package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlueprintEntity
import com.example.data.CostBreakdown
import com.example.ui.audio.AudioNarrationPlayerBar
import com.example.ui.audio.rememberAudioNarrator
import com.example.ui.charts.*
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BlueprintCyan
import java.text.NumberFormat
import java.util.*

// Material Design 3 Stage Visualization Colors
private val StageSubstructureColor = Color(0xFFEC4899)  // Vibrant Coral Pink
private val StageSuperstructureColor = Color(0xFF00B4D8) // Architectural Blueprint Cyan
private val StageRoofingColor = Color(0xFF8B5CF6)       // Deep Amethyst Violet
private val StageFinishesColor = Color(0xFF10B981)      // Emerald Mint
private val StageServicesColor = Color(0xFFF59E0B)      // Warm Amber Gold
private val StageContingencyColor = Color(0xFF64748B)   // Slate Blue Gray

data class MaterialEstimateItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val pricePerUnit: Double,
    val specDetails: String,
    val supplier: String
) {
    val totalCost: Double get() = quantity * pricePerUnit
}

object BoQCalculator {
    fun calculateBoQ(areaSqm: Float): List<MaterialEstimateItem> {
        return listOf(
            MaterialEstimateItem(
                name = "Cement (50kg bags)",
                quantity = areaSqm * 3.7,
                unit = "Bags",
                pricePerUnit = 750.0,
                specDetails = "Bamburi Tembo / Savannah CEM IV 32.5R",
                supplier = "Bamburi Cement Ltd"
            ),
            MaterialEstimateItem(
                name = "Fine River Sand",
                quantity = areaSqm * 0.25,
                unit = "Tonnes",
                pricePerUnit = 1800.0,
                specDetails = "Silt-free local river sand for plaster & mortar",
                supplier = "Machakos Sand Distributors"
            ),
            MaterialEstimateItem(
                name = "Coarse Aggregate / Ballast",
                quantity = areaSqm * 0.2,
                unit = "Tonnes",
                pricePerUnit = 2200.0,
                specDetails = "Machine-crushed basalt, 20mm grade",
                supplier = "Diani Ballast Suppliers"
            ),
            MaterialEstimateItem(
                name = "Quarry Stones (9x9x18\")",
                quantity = areaSqm * 12.0,
                unit = "Pcs",
                pricePerUnit = 40.0,
                specDetails = "Grade A hand-dressed concrete solid blocks",
                supplier = "Ndarugu Quarry Operators"
            ),
            MaterialEstimateItem(
                name = "Steel Reinforcement Rebars",
                quantity = areaSqm * 8.0,
                unit = "Kg",
                pricePerUnit = 135.0,
                specDetails = "High-yield hot-rolled ribbed rebars (Y10/Y12)",
                supplier = "Devki Steel Mills Ltd"
            ),
            MaterialEstimateItem(
                name = "Cypress Roof Timber (2x2 / 2x3)",
                quantity = areaSqm * 11.5,
                unit = "Ft",
                pricePerUnit = 45.0,
                specDetails = "Grade II pressure-treated anti-termite Cypress",
                supplier = "Timsales Timber Yards"
            ),
            MaterialEstimateItem(
                name = "Prepainted Roofing Mabati",
                quantity = areaSqm * 0.75,
                unit = "Sheets",
                pricePerUnit = 1600.0,
                specDetails = "Gauge 28 Box-profile Pre-painted IT5 sheets",
                supplier = "Mabati Rolling Mills (MRM)"
            ),
            MaterialEstimateItem(
                name = "Premium Paint & Primers",
                quantity = areaSqm * 1.5,
                unit = "Litres",
                pricePerUnit = 450.0,
                specDetails = "Acrylic emulsion undercoat & vinyl matt finishes",
                supplier = "Crown Paints Kenya PLC"
            )
        )
    }
}

data class CostItemInfo(
    val title: String,
    val cost: Double,
    val pct: Float,
    val color: Color,
    val icon: ImageVector,
    val notes: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostBreakdownScreen(
    breakdown: CostBreakdown,
    totalAreaSqm: Float,
    blueprint: BlueprintEntity? = null
) {
    val context = LocalContext.current
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
            maximumFractionDigits = 0
        }
    }

    var selectedTabState by remember { mutableIntStateOf(0) } // 0: Charts Dashboard, 1: Stage Breakdown, 2: Material BoQ, 3: Sensitivity Simulator

    val items = remember(breakdown) {
        listOf(
            CostItemInfo("Substructure (Foundation & Slab)", breakdown.substructure, 0.18f, StageSubstructureColor, Icons.Default.Architecture, "Excavations, strip footing, concrete floor bed, hardcore filling."),
            CostItemInfo("Superstructure (Walls & Columns)", breakdown.superstructure, 0.25f, StageSuperstructureColor, Icons.Default.Build, "Quarry stone walling, mortar, lintels, concrete columns & beams."),
            CostItemInfo("Roofing Structure & Cover", breakdown.roofing, 0.15f, StageRoofingColor, Icons.Default.Roofing, "Timber trusses, wall plates, Mabati sheets or roofing tiles."),
            CostItemInfo("Finishes (Paint, Tiles, Plaster)", breakdown.finishes, 0.22f, StageFinishesColor, Icons.Default.Category, "Internal/external plaster, porcelain tiles, sanitary fittings, premium paint."),
            CostItemInfo("Services (Plumbing & Electrical)", breakdown.services, 0.12f, StageServicesColor, Icons.Default.HomeRepairService, "Conduits, wiring, switches, water storage tanks, drainage pipes."),
            CostItemInfo("Contingency & NCA Approvals", breakdown.contingency, 0.08f, StageContingencyColor, Icons.Default.MonetizationOn, "County approvals, NCA registration fee, unexpected site variations.")
        )
    }

    val boqItems = remember(totalAreaSqm) { BoQCalculator.calculateBoQ(totalAreaSqm) }
    val totalBoqMaterialsCost = remember(boqItems) { boqItems.sumOf { it.totalCost } }

    val audioNarrator = rememberAudioNarrator()
    val audioBriefingScript = remember(breakdown, totalAreaSqm, blueprint) {
        val projectName = blueprint?.title ?: "House Layout"
        "Executive Construction Cost Briefing for $projectName. Total estimated building budget ranges from ${formatKsh(breakdown.totalCostMin, currencyFormat)} to ${formatKsh(breakdown.totalCostMax, currencyFormat)} for a total area of ${totalAreaSqm.toInt()} square meters. Budget allocation breakdown: Substructure foundation and slab at 18 percent, Superstructure masonry walls at 25 percent, Roofing structure at 15 percent, Finishes including plaster, paint and tiles at 22 percent, Plumbing and Electrical services at 12 percent, and Contingency with NCA compliance fees at 8 percent."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Audio Briefing Control Bar
        AudioNarrationPlayerBar(
            narrator = audioNarrator,
            title = "Executive Audio Briefing",
            narrationScript = audioBriefingScript
        )

        // Core Total Budget Overview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "ESTIMATED TOTAL BUDGET RANGE",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${formatKsh(breakdown.totalCostMin, currencyFormat)} - ${formatKsh(breakdown.totalCostMax, currencyFormat)}",
                    color = AccentGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("text_total_budget_range")
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Floor Footprint",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${totalAreaSqm.roundTo(1)} sqm",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "NCA Rate / sqm",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${formatKsh(breakdown.finalRatePerSqm, currencyFormat)}/sqm",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Recharts Multi-Tab Dashboard Navigation Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp,
            modifier = Modifier.testTag("cost_breakdown_tab_row")
        ) {
            Tab(
                selected = selectedTabState == 0,
                onClick = { selectedTabState = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Charts Dashboard", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_dashboard_charts")
            )
            Tab(
                selected = selectedTabState == 1,
                onClick = { selectedTabState = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stage Breakdown", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_financial_costing")
            )
            Tab(
                selected = selectedTabState == 2,
                onClick = { selectedTabState = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BoQ Materials", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_material_boq")
            )
            Tab(
                selected = selectedTabState == 3,
                onClick = { selectedTabState = 3 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulator", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_cost_simulator")
            )
        }

        when (selectedTabState) {
            0 -> {
                // ==================== TAB 0: RECHARTS VISUAL DASHBOARD ====================
                // 1. Recharts-inspired Interactive Donut Chart
                RechartsDonutChart(
                    breakdown = breakdown,
                    currencyFormat = currencyFormat
                )

                // 2. Recharts-style Cartesian Bar Chart
                RechartsBarChart(
                    breakdown = breakdown,
                    currencyFormat = currencyFormat
                )

                // 3. Recharts Cumulative Cash Flow S-Curve (AreaChart)
                RechartsProjectSCurveChart(
                    breakdown = breakdown,
                    currencyFormat = currencyFormat
                )

                // 4. Recharts Polar Radar Benchmark Chart
                RechartsRadarChart(
                    breakdown = breakdown
                )

                // 5. County Benchmark Index
                CountyBenchmarkComparisonBar(
                    currentRatePerSqm = breakdown.finalRatePerSqm,
                    currencyFormat = currencyFormat
                )
            }

            1 -> {
                // ==================== TAB 1: STAGE ITEMIZATION ====================
                Text(
                    text = "Itemized Stage Cost Breakdown",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )

                items.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = item.color.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = item.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = item.color.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${(item.pct * 100).toInt()}%",
                                        color = item.color,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimated Stage Cost:",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = formatKsh(item.cost, currencyFormat),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { item.pct },
                                color = item.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.notes,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            2 -> {
                // ==================== TAB 2: MATERIAL BOQ ====================
                Text(
                    text = "Physical Material Quantity Estimates",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )

                boqItems.forEach { boq ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = boq.name,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = boq.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = formatKsh(boq.totalCost, currencyFormat),
                                    color = AccentGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Qty: ${boq.quantity.roundTo(1)} ${boq.unit}",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Rate: KSh ${boq.pricePerUnit.toInt()} / ${boq.unit.lowercase().trimEnd('s')}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Spec: ${boq.specDetails}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Supplier: ${boq.supplier}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Materials Cost Summary",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Raw Materials (Estimate): ${formatKsh(totalBoqMaterialsCost, currencyFormat)}",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Raw materials account for approximately ${(totalBoqMaterialsCost / breakdown.totalCostMax * 100).toInt()}% of the projected upper-tier budget. The remainder covers professional services, municipal permits, transport, excavation machinery, and artisan labor.",
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            3 -> {
                // ==================== TAB 3: SCENARIO SIMULATOR ====================
                RechartsCostSensitivitySimulator(
                    baseCostMin = breakdown.totalCostMin,
                    baseCostMax = breakdown.totalCostMax,
                    currencyFormat = currencyFormat
                )
            }
        }

        // Regulatory Info Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "These preliminary cost estimates are compiled based on average National Construction Authority (NCA) regional material costs in Kenya. Actual site prices may vary depending on material brand selections and developer contractor agreements.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // Export PDF Summary Action
        blueprint?.let { bp ->
            Button(
                onClick = {
                    PdfExporter.generatePdfAndShare(context, bp, true)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_pdf_summary_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Report",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Cost & Floor Plan PDF Summary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatKsh(amount: Double, formatter: NumberFormat): String {
    return try {
        formatter.format(amount).replace("String", "").replace("KES", "Ksh").replace("Ksh", "Ksh ")
    } catch (e: Exception) {
        "Ksh ${amount.toInt()}"
    }
}

private fun Float.roundTo(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10f }
    return (this * multiplier).toInt() / multiplier
}

private fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10.0 }
    return (this * multiplier).toLong() / multiplier
}
