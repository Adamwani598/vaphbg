package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BuildingTemplate
import com.example.data.BuildingTemplateCatalog
import com.example.ui.theme.AccentGreen
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingTemplatesCatalogView(
    onSelectTemplateToCustomize: (BuildingTemplate) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
            maximumFractionDigits = 0
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val categories = remember { listOf("All", "Storey Building", "Maisonette", "Bungalow", "Residential", "Commercial", "Apartment") }

    val filteredTemplates = remember(selectedCategory, searchQuery) {
        BuildingTemplateCatalog.templates.filter { t ->
            val matchCategory = if (selectedCategory == "All") true else t.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = if (searchQuery.isBlank()) true else {
                t.title.contains(searchQuery, ignoreCase = true) ||
                t.subtitle.contains(searchQuery, ignoreCase = true) ||
                t.category.contains(searchQuery, ignoreCase = true) ||
                t.badge.contains(searchQuery, ignoreCase = true)
            }
            matchCategory && matchSearch
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Filter Header
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search templates (e.g. Maisonette, Villa, 4 Bed)...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("templates_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Category Filter Chips
        if (isLoading) {
            SkeletonFilterChips(count = 6)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("template_category_chip_$cat")
                    )
                }
            }
        }

        // Header with count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) "Loading Architectural Templates..." else "Architectural Templates (${filteredTemplates.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tap to open & customize",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        // Full Height Scrollable Vertical List or Skeleton Loader
        if (isLoading) {
            SkeletonTemplatesList(
                count = 3,
                isFullWidth = true,
                modifier = Modifier.weight(1f)
            )
        } else if (filteredTemplates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No templates match '$searchQuery'", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("templates_vertical_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTemplates) { template ->
                    BuildingTemplateCard(
                        template = template,
                        currencyFormat = currencyFormat,
                        isFullWidth = true,
                        onCustomize = { onSelectTemplateToCustomize(template) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingTemplatesSection(
    onSelectTemplateToCustomize: (BuildingTemplate) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
            maximumFractionDigits = 0
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All", "Storey Building", "Maisonette", "Bungalow", "Residential", "Commercial", "Apartment") }

    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") {
            BuildingTemplateCatalog.templates
        } else {
            BuildingTemplateCatalog.templates.filter {
                it.category.equals(selectedCategory, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Architectural Templates",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Ready-to-use NCA compliant building models",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${filteredTemplates.size} Ready",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("template_category_chip_$cat")
                )
            }
        }

        // Templates Cards Carousel or Skeleton Loader
        if (isLoading) {
            SkeletonTemplatesList(
                count = 3,
                isFullWidth = false
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("templates_horizontal_list")
            ) {
                items(filteredTemplates) { template ->
                    BuildingTemplateCard(
                        template = template,
                        currencyFormat = currencyFormat,
                        isFullWidth = false,
                        onCustomize = { onSelectTemplateToCustomize(template) }
                    )
                }
            }
        }
    }
}

@Composable
fun BuildingTemplateCard(
    template: BuildingTemplate,
    currencyFormat: NumberFormat,
    isFullWidth: Boolean = false,
    onCustomize: () -> Unit
) {
    Card(
        modifier = if (isFullWidth) Modifier.fillMaxWidth().clickable { onCustomize() }.testTag("template_card_${template.id}")
                   else Modifier.width(280.dp).clickable { onCustomize() }.testTag("template_card_${template.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Category Badge & Popularity Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = template.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    color = when (template.badge) {
                        "Featured", "Most Popular" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        "Luxury" -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                        "Commercial ROI", "Commercial" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = template.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (template.badge) {
                            "Featured", "Most Popular" -> Color(0xFFE65100)
                            "Luxury" -> Color(0xFF6A1B9A)
                            "Commercial ROI", "Commercial" -> Color(0xFF0D47A1)
                            else -> Color(0xFF2E7D32)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Interactive Mini 2D Plan Graphic Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rooms = template.defaultRooms
                    val maxX = rooms.maxOfOrNull { it.x + it.width } ?: 12f
                    val maxY = rooms.maxOfOrNull { it.y + it.height } ?: 10f

                    val scale = min(size.width / (maxX + 1f), size.height / (maxY + 1f))
                    val offX = (size.width - maxX * scale) / 2f
                    val offY = (size.height - maxY * scale) / 2f

                    rooms.forEach { room ->
                        val rx = offX + room.x * scale
                        val ry = offY + room.y * scale
                        val rw = room.width * scale
                        val rh = room.height * scale

                        drawRect(
                            color = parseHexColor(room.color).copy(alpha = 0.55f),
                            topLeft = Offset(rx, ry),
                            size = Size(rw, rh)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.75f),
                            topLeft = Offset(rx, ry),
                            size = Size(rw, rh),
                            style = Stroke(width = 1.0f)
                        )
                    }
                }
            }

            // Title & Subtitle
            Column {
                Text(
                    text = template.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = template.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Specs Row (Bed, Bath, Area, Plot)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (template.isMultiStorey || template.maxStoreys > 1) {
                    SpecPill(icon = Icons.Default.Apartment, label = "Up to ${template.maxStoreys}F")
                }
                if (template.bedrooms > 0) {
                    SpecPill(icon = Icons.Default.Bed, label = "${template.bedrooms} Beds")
                }
                SpecPill(icon = Icons.Default.Bathtub, label = "${template.bathrooms} Baths")
                SpecPill(
                    icon = Icons.Default.SquareFoot,
                    label = if (template.isMultiStorey) "${template.baseAreaSqm.roundToInt()} m²/F" else "${template.baseAreaSqm.roundToInt()} m²"
                )
            }

            // Cost & Action Button Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Est. Build Cost:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatCompactCost(template.estimatedCostMinKes)} – ${formatCompactCost(template.estimatedCostMaxKes)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onCustomize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("customize_template_btn_${template.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open & Customize", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SpecPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCompactCost(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format(Locale.US, "KES %.1fM", amount / 1_000_000.0)
        amount >= 1_000 -> String.format(Locale.US, "KES %.0fK", amount / 1_000.0)
        else -> String.format(Locale.US, "KES %.0f", amount)
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.replace("#", "")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}
