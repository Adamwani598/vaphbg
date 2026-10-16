package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BlueprintEntity
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.MutedSlate
import com.example.ui.BlueprintViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import android.content.Intent
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Tune
import com.example.data.BuildingTemplate
import com.example.data.BuildingTemplateCatalog
import com.example.ui.components.BuildingTemplatesCatalogView
import com.example.ui.components.BuildingTemplatesSection
import com.example.ui.components.TemplateCustomizationDialog
import com.example.ui.components.SkeletonBlueprintsList
import com.example.ui.components.SkeletonTemplatesList
import com.example.ui.components.SkeletonKpiRibbon
import com.example.ui.components.SkeletonFilterChips
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BlueprintViewModel,
    onCreateNewBlueprint: () -> Unit,
    onSelectBlueprint: (BlueprintEntity) -> Unit,
    onViewOnboarding: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenLiveCamera: (CameraCaptureMode) -> Unit = {},
    onOpenMediaGallery: () -> Unit = {}
) {
    val blueprints by viewModel.allBlueprints.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isLoadingDashboard by viewModel.isLoadingDashboard.collectAsState()

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
        maximumFractionDigits = 0
    }}

    var isComparisonMode by remember { mutableStateOf(false) }
    val selectedCompareIds = remember { mutableStateListOf<Long>() }
    var showComparisonDialog by remember { mutableStateOf(false) }
    var selectedDashboardTab by remember { mutableIntStateOf(0) } // 0: My Designs, 1: Architectural Templates

    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    var editingTagsBlueprint by remember { mutableStateOf<BlueprintEntity?>(null) }
    var selectedTemplateForCustomization by remember { mutableStateOf<BuildingTemplate?>(null) }

    val categoryOptions = remember { listOf("All", "Residential", "Commercial", "Bungalow", "Maisonette", "Apartment", "Custom") }

    val filteredBlueprints = remember(blueprints, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            blueprints
        } else {
            blueprints.filter { bp ->
                bp.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                        bp.tags.contains(selectedCategoryFilter, ignoreCase = true)
            }
        }
    }

    if (editingTagsBlueprint != null) {
        ManageTagsDialog(
            blueprint = editingTagsBlueprint!!,
            onSave = { category, tags ->
                viewModel.updateBlueprintTagsAndCategory(editingTagsBlueprint!!.id, category, tags)
                editingTagsBlueprint = null
            },
            onDismiss = { editingTagsBlueprint = null }
        )
    }

    selectedTemplateForCustomization?.let { template ->
        TemplateCustomizationDialog(
            template = template,
            onDismiss = { selectedTemplateForCustomization = null },
            onConfirmCustomization = { customTitle, customAreaSqm, county, finishesQuality, roofType, includedModules, storeys ->
                viewModel.applyBuildingTemplate(
                    template = template,
                    customTitle = customTitle,
                    customAreaSqm = customAreaSqm,
                    county = county,
                    finishesQuality = finishesQuality,
                    roofType = roofType,
                    includedModules = includedModules,
                    storeys = storeys
                ) { createdBlueprint ->
                    selectedTemplateForCustomization = null
                    onSelectBlueprint(createdBlueprint)
                }
            }
        )
    }

    if (showComparisonDialog && selectedCompareIds.size == 2) {
        val bp1 = blueprints.find { it.id == selectedCompareIds[0] }
        val bp2 = blueprints.find { it.id == selectedCompareIds[1] }
        if (bp1 != null && bp2 != null) {
            BlueprintComparisonDialog(
                blueprint1 = bp1,
                blueprint2 = bp2,
                currencyFormat = currencyFormat,
                onDismiss = {
                    showComparisonDialog = false
                    selectedCompareIds.clear()
                    isComparisonMode = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clickable { onOpenProfile() }
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HomeWork,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "VAPHBG Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            currentUser?.let {
                                Text(
                                    text = "${it.name} • ${it.role.take(16)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onOpenLiveCamera(CameraCaptureMode.SKETCH) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("open_camera_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "CameraX Floor Plan Scanner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshDashboard() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("refresh_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Dashboard",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenMediaGallery,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("dashboard_media_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Captured Media Gallery",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleDarkMode(!isDarkMode) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("open_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My Profile & Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onViewOnboarding,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("onboarding_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "View Guide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Hero Welcome & Portfolio KPI Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Jambo, ${currentUser?.name?.split(" ")?.firstOrNull() ?: "Contractor"}! 👋",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "NCA Compliant Kenyan House Floorplans & Estimator",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { onOpenLiveCamera(CameraCaptureMode.SKETCH) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("hero_camera_scan_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onCreateNewBlueprint,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("hero_create_blueprint_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingDashboard) {
                        SkeletonKpiRibbon()
                    } else {
                        // 3 KPI Portfolio Metric Ribbon
                        val totalProjectsCount = blueprints.size
                        val totalAreaSum = remember(blueprints) { blueprints.sumOf { it.totalAreaSqm.toDouble() }.roundToInt() }
                        val totalAvgCost = remember(blueprints) {
                            blueprints.sumOf { (it.estimatedCostMinKes + it.estimatedCostMaxKes) / 2.0 }.toLong()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Designs", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Text("$totalProjectsCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Total Area", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Text("$totalAreaSum m²", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1.3f),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Est. Portfolio Value", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Text(formatKsh(totalAvgCost, currencyFormat), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                }
                            }
                        }
                    }
                }
            }

            // Dedicated Tabs Switcher Bar: My Designs vs Architectural Templates
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedDashboardTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedDashboardTab]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedDashboardTab == 0,
                        onClick = {
                            selectedDashboardTab = 0
                            isComparisonMode = false
                            selectedCompareIds.clear()
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = if (selectedDashboardTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "My Designs",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedDashboardTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDashboardTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = if (selectedDashboardTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${blueprints.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDashboardTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_my_designs")
                    )

                    Tab(
                        selected = selectedDashboardTab == 1,
                        onClick = {
                            selectedDashboardTab = 1
                            isComparisonMode = false
                            selectedCompareIds.clear()
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DashboardCustomize,
                                    contentDescription = null,
                                    tint = if (selectedDashboardTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Templates",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedDashboardTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedDashboardTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = if (selectedDashboardTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${BuildingTemplateCatalog.templates.size} Ready",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDashboardTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_building_templates")
                    )
                }
            }

            if (selectedDashboardTab == 1) {
                // Architectural Building Templates Catalog Tab with Full Vertical Scrolling
                BuildingTemplatesCatalogView(
                    onSelectTemplateToCustomize = { template ->
                        selectedTemplateForCustomization = template
                    },
                    isLoading = isLoadingDashboard,
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (isLoadingDashboard) {
                    // Skeleton Screen for My Designs tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeletonFilterChips(count = 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Loading Your House Designs...",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        SkeletonBlueprintsList(
                            count = 3,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Category & Tag Filter Chips Bar for My Designs
                    if (blueprints.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categoryOptions) { category ->
                                FilterChip(
                                    selected = selectedCategoryFilter == category,
                                    onClick = { viewModel.setCategoryFilter(category) },
                                    label = {
                                        Text(
                                            text = category,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedCategoryFilter == category) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (category != "All") {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Label,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.testTag("filter_chip_$category")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isComparisonMode) "Select 2 Blueprints to Compare" else "Your House Designs (${filteredBlueprints.size}${if (selectedCategoryFilter != "All") " in $selectedCategoryFilter" else ""})",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        if (blueprints.size >= 2) {
                            TextButton(
                                onClick = {
                                    isComparisonMode = !isComparisonMode
                                    selectedCompareIds.clear()
                                },
                                modifier = Modifier.testTag("toggle_comparison_mode_button")
                            ) {
                                Text(
                                    text = if (isComparisonMode) "Exit Compare" else "Compare Layouts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (filteredBlueprints.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (blueprints.isEmpty()) "No custom designs generated yet." else "No designs match category '$selectedCategoryFilter'.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (blueprints.isEmpty()) {
                                        Button(
                                            onClick = onCreateNewBlueprint,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("New Design")
                                        }
                                        OutlinedButton(
                                            onClick = { selectedDashboardTab = 1 },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Browse Templates")
                                        }
                                    } else {
                                        TextButton(onClick = { viewModel.setCategoryFilter("All") }) {
                                            Text("Show All Designs")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("blueprints_vertical_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredBlueprints) { blueprint ->
                            val isSelected = selectedCompareIds.contains(blueprint.id)
                            val context = LocalContext.current

                            if (isComparisonMode) {
                                BlueprintCardItem(
                                    blueprint = blueprint,
                                    isSelected = isSelected,
                                    isComparisonMode = true,
                                    dateFormat = dateFormat,
                                    currencyFormat = currencyFormat,
                                    onSelectBlueprint = onSelectBlueprint,
                                    onToggleCompare = {
                                        if (isSelected) {
                                            selectedCompareIds.remove(blueprint.id)
                                        } else if (selectedCompareIds.size < 2) {
                                            selectedCompareIds.add(blueprint.id)
                                        }
                                    },
                                    onManageTags = { editingTagsBlueprint = blueprint },
                                    onDelete = { viewModel.deleteBlueprint(blueprint.id) }
                                )
                            } else {
                                SwipeToRevealItem(
                                    blueprintId = blueprint.id,
                                    onShare = {
                                        val shareText = "Check out this floor plan: ${blueprint.title}\n" +
                                                "County: ${blueprint.county} County\n" +
                                                "Total Footprint Area: ${blueprint.totalAreaSqm.roundTo(1)} sqm\n" +
                                                "Estimated Cost: ${formatKsh(blueprint.estimatedCostMinKes, currencyFormat)} - ${formatKsh(blueprint.estimatedCostMaxKes, currencyFormat)}\n" +
                                                "Created with AI Floorplan Generator."
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, blueprint.title)
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Blueprint"))
                                    },
                                    onDuplicate = {
                                        viewModel.duplicateBlueprint(blueprint.id)
                                    },
                                    onDelete = {
                                        viewModel.deleteBlueprint(blueprint.id)
                                    }
                                ) {
                                    BlueprintCardItem(
                                        blueprint = blueprint,
                                        isSelected = isSelected,
                                        isComparisonMode = false,
                                        dateFormat = dateFormat,
                                        currencyFormat = currencyFormat,
                                        onSelectBlueprint = onSelectBlueprint,
                                        onToggleCompare = {
                                            if (isSelected) {
                                                selectedCompareIds.remove(blueprint.id)
                                            } else if (selectedCompareIds.size < 2) {
                                                selectedCompareIds.add(blueprint.id)
                                            }
                                        },
                                        onManageTags = { editingTagsBlueprint = blueprint },
                                        onDelete = { viewModel.deleteBlueprint(blueprint.id) }
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

            // Floating bottom bar for actioning comparison
            if (isComparisonMode && selectedCompareIds.size > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected: ${selectedCompareIds.size} / 2",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { showComparisonDialog = true },
                            enabled = selectedCompareIds.size == 2,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("compare_selected_layouts_button")
                        ) {
                            Icon(imageVector = Icons.Default.CompareArrows, contentDescription = "Compare")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compare Selected")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlueprintCardItem(
    blueprint: BlueprintEntity,
    isSelected: Boolean,
    isComparisonMode: Boolean,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onSelectBlueprint: (BlueprintEntity) -> Unit,
    onToggleCompare: () -> Unit,
    onManageTags: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isComparisonMode) {
                    onToggleCompare()
                } else {
                    onSelectBlueprint(blueprint)
                }
            }
            .testTag("blueprint_item_card_${blueprint.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Title & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = blueprint.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = blueprint.category,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    blueprint.getTagList().take(1).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Row: Thumbnail + Info + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isComparisonMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleCompare() },
                        modifier = Modifier.testTag("compare_checkbox_${blueprint.id}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        BlueprintThumbnail(
                            roomsJson = blueprint.roomsJson,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${blueprint.totalAreaSqm.roundTo(1)} m² • ${blueprint.plotSize}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "County: ${blueprint.county}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cost Estimate Pill
                    Surface(
                        color = AccentGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Est: ${formatKsh(blueprint.estimatedCostMinKes, currencyFormat)} - ${formatKsh(blueprint.estimatedCostMaxKes, currencyFormat)}",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!isComparisonMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onManageTags,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("manage_tags_button_${blueprint.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = "Manage Tags & Category",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_blueprint_button_${blueprint.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(6.dp))

            // Footer Row: Timestamp & NCA Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Created: ${dateFormat.format(Date(blueprint.createdAt))}",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "3D Ready • NCA Compliant",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun BlueprintComparisonDialog(
    blueprint1: BlueprintEntity,
    blueprint2: BlueprintEntity,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            color = Color(0xFF0D1B2A), // Cosmic slate matching app aesthetic
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Compare icon",
                            tint = Color(0xFF00B4D8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Side-by-Side Design Comparison",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick stats header cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = blueprint1.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${blueprint1.totalAreaSqm.roundTo(1)} sqm",
                                    color = Color(0xFF00B4D8),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Est. Max: ${formatKsh(blueprint1.estimatedCostMaxKes, currencyFormat)}",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = blueprint2.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${blueprint2.totalAreaSqm.roundTo(1)} sqm",
                                    color = Color(0xFF00B4D8),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Est. Max: ${formatKsh(blueprint2.estimatedCostMaxKes, currencyFormat)}",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Metric rows
                    ComparisonMetricHeader(title = "ARCHITECTURAL OVERVIEW")
                    
                    ComparisonMetricRow(
                        label = "Footprint Area",
                        val1 = "${blueprint1.totalAreaSqm.roundTo(1)} sqm",
                        val2 = "${blueprint2.totalAreaSqm.roundTo(1)} sqm",
                        betterIdx = if (blueprint1.totalAreaSqm > blueprint2.totalAreaSqm) 1 else 2,
                        subText = "Larger area provides more living space"
                    )

                    ComparisonMetricRow(
                        label = "Cost Range (KES)",
                        val1 = "${formatKsh(blueprint1.estimatedCostMinKes, currencyFormat)} - ${formatKsh(blueprint1.estimatedCostMaxKes, currencyFormat)}",
                        val2 = "${formatKsh(blueprint2.estimatedCostMinKes, currencyFormat)} - ${formatKsh(blueprint2.estimatedCostMaxKes, currencyFormat)}",
                        betterIdx = if (blueprint1.estimatedCostMaxKes < blueprint2.estimatedCostMaxKes) 1 else 2,
                        subText = "Lower budget layout is more cost-effective"
                    )

                    ComparisonMetricRow(
                        label = "Plot Footprint Coverage",
                        val1 = "${((blueprint1.totalAreaSqm / 464.5f) * 100).roundTo(1)}%",
                        val2 = "${((blueprint2.totalAreaSqm / 464.5f) * 100).roundTo(1)}%",
                        betterIdx = if (blueprint1.totalAreaSqm < blueprint2.totalAreaSqm) 1 else 2,
                        subText = "Based on standard 50x100 ft (464.5 sqm) local plot"
                    )

                    // Materials estimates side-by-side (Physical Material Quantity Estimator)
                    ComparisonMetricHeader(title = "ESTIMATED PHYSICAL MATERIALS (BoQ)")
                    
                    val cem1 = (blueprint1.totalAreaSqm * 3.7).toInt()
                    val cem2 = (blueprint2.totalAreaSqm * 3.7).toInt()
                    ComparisonMetricRow(
                        label = "Cement Bags (50kg)",
                        val1 = "$cem1 Bags",
                        val2 = "$cem2 Bags",
                        betterIdx = if (cem1 < cem2) 1 else 2,
                        subText = "For foundation slab, masonry bonding, & plaster"
                    )

                    val st1 = (blueprint1.totalAreaSqm * 8.0).toInt()
                    val st2 = (blueprint2.totalAreaSqm * 8.0).toInt()
                    ComparisonMetricRow(
                        label = "Reinforcement Steel",
                        val1 = "$st1 Kg",
                        val2 = "$st2 Kg",
                        betterIdx = if (st1 < st2) 1 else 2,
                        subText = "High-yield KEBS approved concrete rebars"
                    )

                    val mb1 = (blueprint1.totalAreaSqm * 0.75).toInt()
                    val mb2 = (blueprint2.totalAreaSqm * 0.75).toInt()
                    ComparisonMetricRow(
                        label = "Mabati Roof Sheets",
                        val1 = "$mb1 Pcs",
                        val2 = "$mb2 Pcs",
                        betterIdx = if (mb1 < mb2) 1 else 2,
                        subText = "Gauge 28 Pre-painted Box profile roofing sheets"
                    )

                    val stn1 = (blueprint1.totalAreaSqm * 12.0).toInt()
                    val stn2 = (blueprint2.totalAreaSqm * 12.0).toInt()
                    ComparisonMetricRow(
                        label = "Walling Quarry Stones",
                        val1 = "$stn1 Pcs",
                        val2 = "$stn2 Pcs",
                        betterIdx = if (stn1 < stn2) 1 else 2,
                        subText = "Grade A hand-dressed quarry stones"
                    )

                    ComparisonMetricHeader(title = "LAYOUT & CONFIGURATION")
                    
                    ComparisonRoomsRow(
                        blueprint1 = blueprint1,
                        blueprint2 = blueprint2
                    )

                    ComparisonMetricHeader(title = "REGIONAL COUNTY ZONING & COMPLIANCE")
                    
                    ComparisonMetricRow(
                        label = "Kenyan County",
                        val1 = blueprint1.county,
                        val2 = blueprint2.county,
                        betterIdx = 0,
                        subText = "Governing regional physical planning authority"
                    )

                    val preset1 = com.example.data.CountyZoningPresets.getPreset(blueprint1.county)
                    val preset2 = com.example.data.CountyZoningPresets.getPreset(blueprint2.county)

                    ComparisonMetricRow(
                        label = "Zoning Max Coverage",
                        val1 = "${preset1.maxCoveragePct}% Allowed",
                        val2 = "${preset2.maxCoveragePct}% Allowed",
                        betterIdx = if (preset1.maxCoveragePct > preset2.maxCoveragePct) 1 else 2,
                        subText = "Maximum permissible plot area built-up"
                    )

                    ComparisonMetricRow(
                        label = "Zoning Min Setback",
                        val1 = "${preset1.minSetbackMeters} m",
                        val2 = "${preset2.minSetbackMeters} m",
                        betterIdx = if (preset1.minSetbackMeters < preset2.minSetbackMeters) 1 else 2,
                        subText = "Minimum distance from boundaries"
                    )

                    ComparisonMetricRow(
                        label = "Sewage Standard",
                        val1 = preset1.sewageRequirement,
                        val2 = preset2.sewageRequirement,
                        betterIdx = 0,
                        subText = "Mandated county environmental sewage treatment"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Comparison", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ComparisonMetricHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF00B4D8),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ComparisonMetricRow(
    label: String,
    val1: String,
    val2: String,
    betterIdx: Int,
    subText: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = val1,
                        color = if (betterIdx == 1) AccentGreen else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (betterIdx == 1) FontWeight.Bold else FontWeight.Normal
                    )
                    if (betterIdx == 1) {
                        Text("Advantage", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = val2,
                        color = if (betterIdx == 2) AccentGreen else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (betterIdx == 2) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.End
                    )
                    if (betterIdx == 2) {
                        Text("Advantage", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (subText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• $subText",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ComparisonRoomsRow(
    blueprint1: BlueprintEntity,
    blueprint2: BlueprintEntity
) {
    val rooms1 = remember(blueprint1) { parseRoomsList(blueprint1.roomsJson) }
    val rooms2 = remember(blueprint2) { parseRoomsList(blueprint2.roomsJson) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Room-by-Room Composition", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${rooms1.size} Rooms:", color = Color(0xFF00B4D8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    rooms1.forEach { r ->
                        Text(
                            text = "• ${r.name} (${r.width.roundTo(1)}m × ${r.height.roundTo(1)}m)",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = "${rooms2.size} Rooms:", color = Color(0xFF00B4D8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    rooms2.forEach { r ->
                        Text(
                            text = "• ${r.name} (${r.width.roundTo(1)}m × ${r.height.roundTo(1)}m)",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

private fun parseRoomsList(roomsJson: String): List<com.example.data.RoomItem> {
    val list = mutableListOf<com.example.data.RoomItem>()
    try {
        val arr = org.json.JSONArray(roomsJson)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                com.example.data.RoomItem(
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
        list.add(com.example.data.RoomItem("Living Room", 5f, 4f, 0f, 0f))
    }
    return list
}

private fun formatKsh(amount: Number, formatter: NumberFormat): String {
    return try {
        // Kenyan standard display KSh 4,500,000
        formatter.format(amount.toDouble()).replace("KES", "Ksh").replace("Ksh", "Ksh ")
    } catch (e: Exception) {
        "Ksh ${amount.toLong()}"
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
    return (this * multiplier).toInt() / multiplier
}

@Composable
fun BlueprintThumbnail(roomsJson: String, modifier: Modifier = Modifier) {
    val rooms = remember(roomsJson) { parseRoomsList(roomsJson) }

    Box(
        modifier = modifier
            .background(Color(0xFF1B263B), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        if (rooms.isEmpty()) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val drawWidth = size.width
                val drawHeight = size.height
                
                val maxRoomX = rooms.maxOfOrNull { it.x + it.width } ?: 10f
                val maxRoomY = rooms.maxOfOrNull { it.y + it.height } ?: 10f
                
                val scaleFactor = (drawWidth * 0.85f / maxRoomX).coerceAtMost(drawHeight * 0.85f / maxRoomY).coerceIn(2f, 15f)
                
                val startX = (drawWidth - (maxRoomX * scaleFactor)) / 2f
                val startY = (drawHeight - (maxRoomY * scaleFactor)) / 2f
                
                rooms.forEach { room ->
                    val rx = startX + room.x * scaleFactor
                    val ry = startY + room.y * scaleFactor
                    val rw = room.width * scaleFactor
                    val rh = room.height * scaleFactor
                    
                    val roomColor = try {
                        Color(android.graphics.Color.parseColor(room.color))
                    } catch (e: Exception) {
                        Color(0xFF2196F3)
                    }
                    
                    drawRect(
                        color = roomColor.copy(alpha = 0.4f),
                        topLeft = Offset(rx, ry),
                        size = Size(rw, rh)
                    )
                    drawRect(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(rx, ry),
                        size = Size(rw, rh),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealItem(
    blueprintId: Long,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionsWidthPx = remember(density) { with(density) { 192.dp.toPx() } }
    
    var dragOffset by remember { mutableStateOf(0f) }
    var isRevealed by remember { mutableStateOf(false) }
    
    LaunchedEffect(blueprintId) {
        dragOffset = 0f
        isRevealed = false
    }
    
    val targetOffset = if (isRevealed) -actionsWidthPx else dragOffset

    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SwipeOffset"
    )

    val revealFraction = (-animatedOffset / actionsWidthPx).coerceIn(0f, 1f)

    val actionScale by animateFloatAsState(
        targetValue = if (revealFraction > 0.1f) 0.85f + (revealFraction * 0.15f) else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ActionScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        isRevealed = false
                        dragOffset = 0f
                        onShare()
                    }
                    .testTag("swipe_action_share_$blueprintId"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.scale(actionScale)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Share",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .background(AccentGreen)
                    .clickable {
                        isRevealed = false
                        dragOffset = 0f
                        onDuplicate()
                    }
                    .testTag("swipe_action_duplicate_$blueprintId"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.scale(actionScale)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Clone",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable {
                        isRevealed = false
                        dragOffset = 0f
                        onDelete()
                    }
                    .testTag("swipe_action_delete_$blueprintId"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.scale(actionScale)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(blueprintId) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            val currentBase = if (isRevealed) -actionsWidthPx else dragOffset
                            val rawOffset = currentBase + dragAmount
                            dragOffset = when {
                                rawOffset > 0f -> rawOffset * 0.3f
                                rawOffset < -actionsWidthPx -> -actionsWidthPx + (rawOffset + actionsWidthPx) * 0.3f
                                else -> rawOffset
                            }
                        },
                        onDragEnd = {
                            if (dragOffset < -actionsWidthPx / 3f) {
                                isRevealed = true
                                dragOffset = -actionsWidthPx
                            } else {
                                isRevealed = false
                                dragOffset = 0f
                            }
                        },
                        onDragCancel = {
                            isRevealed = false
                            dragOffset = 0f
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTagsDialog(
    blueprint: BlueprintEntity,
    onSave: (category: String, tags: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(blueprint.category) }
    var tagsInput by remember { mutableStateOf(blueprint.tags) }
    val presetCategories = remember { listOf("Residential", "Commercial", "Bungalow", "Maisonette", "Apartment", "Custom") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Organize Floorplan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Custom Tags (comma separated)") },
                    placeholder = { Text("e.g. Modern, 3-Bedroom, Budget") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_tags_input"),
                    shape = RoundedCornerShape(10.dp)
                )
                Text(
                    text = "Tags allow quick filtering on the dashboard.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedCategory, tagsInput)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_tags_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
