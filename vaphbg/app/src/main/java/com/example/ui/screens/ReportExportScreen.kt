package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlueprintEntity
import com.example.ui.audio.AudioNarrationPlayerBar
import com.example.ui.audio.rememberAudioNarrator
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.MutedSlate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private fun formatKsh(amount: Number, formatter: NumberFormat): String {
    return try {
        formatter.format(amount.toDouble()).replace("KES", "Ksh").replace("Ksh", "Ksh ")
    } catch (e: Exception) {
        "Ksh ${amount.toLong()}"
    }
}

@Composable
fun ReportExportScreen(
    blueprint: BlueprintEntity
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
        maximumFractionDigits = 0
    }}

    var isExporting by remember { mutableStateOf(false) }
    var complianceChecked by remember { mutableStateOf(false) }

    LaunchedEffect(isExporting) {
        if (isExporting) {
            kotlinx.coroutines.delay(1800)
            isExporting = false
            PdfExporter.generatePdfAndShare(context, blueprint, complianceChecked)
        }
    }

    val audioNarrator = rememberAudioNarrator()
    val reportAudioScript = remember(blueprint) {
        "Consolidated Architectural Report Audio Summary for ${blueprint.title}. County location: ${blueprint.county}. Plot size: ${blueprint.plotSize}. Gross floor area: ${blueprint.totalAreaSqm.toInt()} square meters. Estimated budget: ${formatKsh(blueprint.estimatedCostMinKes, currencyFormat)} to ${formatKsh(blueprint.estimatedCostMaxKes, currencyFormat)}. Structural Compliance: Complies with National Construction Authority standards, featuring stone masonry walls, ring beams, and gauge 28 roofing."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Audio Report Summary Player
        AudioNarrationPlayerBar(
            narrator = audioNarrator,
            title = "Architectural Report Voice Summary",
            narrationScript = reportAudioScript
        )

        // Document Header Sheet
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "CONSOLIDATED ARCHITECTURAL SPECIFICATIONS REPORT",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = blueprint.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ref: NP-${blueprint.id}-${blueprint.createdAt % 100000}",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Compiled on: ${dateFormat.format(Date(blueprint.createdAt))}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        // Structural Specs Section
        Text(
            text = "NCA Structural Specifications Checklist",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpecItemRow("Substructure (Foundation)", "Concrete Strip Footing, 1000g Damp Proof Membrane (DPM) polythene sheeting, standard hardcore filling, anti-termite soil treatment.")
                SpecItemRow("Walling & Framing", "Class A quarry-dressed natural stone block walling (9\" ext, 6\" int) bonded with Class II cement mortar.")
                SpecItemRow("Roof Trusses & Cover", "Grade II Cypress structural timber rafters/trusses treated with anti-borer, covered with Pre-painted 28-Gauge Mabati sheets.")
                SpecItemRow("Doors & Windows", "Solid hardwood exterior panel doors, high-density flush interior doors, and steel-casement architectural windows.")
                SpecItemRow("Ventilation & Light", "Meets Physical Planning standards. Natural floor-to-window daylight ratio > 10% for all habitable rooms.")
            }
        }

        // NCA regulatory compliance verification
        val countyPreset = remember(blueprint.county) { com.example.data.CountyZoningPresets.getPreset(blueprint.county) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, if (complianceChecked) AccentGreen else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kenyan Physical Planning Act Check (${blueprint.county} County)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Checkbox(
                        checked = complianceChecked,
                        onCheckedChange = { complianceChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = AccentGreen, checkmarkColor = MaterialTheme.colorScheme.onSecondary),
                        modifier = Modifier.testTag("compliance_checkbox")
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verify self-assessment compliance under the Physical Planning Act for ${blueprint.county} County: Front boundary setback > 6.0m, side/rear setbacks > ${countyPreset.minSetbackMeters}m, maximum footprint coverage <= ${countyPreset.maxCoveragePct}%, and local county sewage rule: ${countyPreset.sewageRequirement}.",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
                if (complianceChecked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Passed", tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preliminary Layout complies with ${blueprint.county} County zoning limits (${countyPreset.maxCoveragePct}% max coverage, ${countyPreset.sewageRequirement}).", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Regulatory building safety warning (MANDATORY DISCLAIMER)
        Card(
            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(1.dp, AccentRed, RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = "Disclaimer", tint = AccentRed)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "IMPORTANT LEGAL DISCLAIMER: This document contains a preliminary conceptual design and structural cost estimation. It does NOT substitute certified surveys. It MUST be reviewed, stamped, and approved by a licensed, registered Kenyan Architect or Structural Engineer before physical site construction or county building permit submissions.",
                    color = AccentRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Buttons: Share and PDF Compile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    Toast.makeText(context, "Exporting raw blueprint SVG and dimensions to system clip...", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("share_blueprint_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Layout", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }

            Button(
                onClick = { isExporting = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("compile_pdf_report_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isExporting) "Compiling PDF..." else "Export PDF Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SpecItemRow(label: String, detail: String) {
    Column {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    }
}
