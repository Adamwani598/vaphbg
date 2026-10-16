package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlueprintEntity
import com.example.data.UserEntity
import com.example.ui.BlueprintViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: BlueprintViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onViewOnboarding: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allBlueprints by viewModel.allBlueprints.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showComplianceDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var selectedTutorialItem by remember { mutableStateOf<TutorialItem?>(null) }

    val user = currentUser ?: UserEntity(
        email = "guest@nyumbaplan.co.ke",
        name = "Guest Architect",
        passcodeHash = ""
    )

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text("Log Out & Clear Session", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to log out of NyumbaPlan? Your active local session will be cleared and you will be redirected to the landing sign-in screen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                        Toast.makeText(context, "Logged out successfully. Local session cleared.", Toast.LENGTH_SHORT).show()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_logout_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            user = user,
            onSave = { name, role, phone, county ->
                viewModel.updateUserProfile(name, role, phone, county)
                showEditProfileDialog = false
                Toast.makeText(context, "Profile details updated!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onSave = { currentPin, newPin, callback ->
                viewModel.updateSecurityPin(currentPin, newPin) { success, msg ->
                    callback(success, msg)
                    if (success) {
                        showChangePinDialog = false
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showComplianceDialog) {
        BuildingComplianceDialog(onDismiss = { showComplianceDialog = false })
    }

    if (showPrivacyDialog) {
        PrivacyTermsDialog(onDismiss = { showPrivacyDialog = false })
    }

    selectedTutorialItem?.let { tutorial ->
        TutorialDetailDialog(
            tutorial = tutorial,
            onDismiss = { selectedTutorialItem = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "User Profile & Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode(!isDarkMode) },
                        modifier = Modifier.testTag("profile_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier.testTag("profile_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Header & Identity Card
            UserHeaderCard(
                user = user,
                onEditClick = { showEditProfileDialog = true },
                onAvatarColorChange = { colorHex ->
                    viewModel.updateAvatarColor(colorHex)
                }
            )

            // 2. Room Database Portfolio Stats Summary
            PortfolioSummaryCard(
                blueprints = allBlueprints,
                preferredCurrency = user.preferredCurrency,
                onExportData = {
                    val summary = "NyumbaPlan Export\nUser: ${user.name}\nTotal Projects: ${allBlueprints.size}\nSaved Blueprints: ${allBlueprints.joinToString { it.title }}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Blueprint Export", summary)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Projects summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            )

            // 3. Estimator & App Preferences
            PreferencesCard(
                user = user,
                onPreferenceChange = { currency, unit, preset ->
                    viewModel.updateUserPreferences(currency, unit, preset)
                    Toast.makeText(context, "Preferences saved!", Toast.LENGTH_SHORT).show()
                }
            )

            // 3.5. Theme & Appearance Toggle Card
            ThemeCard(
                isDarkMode = isDarkMode,
                onToggleTheme = { enabled ->
                    viewModel.toggleDarkMode(enabled)
                    Toast.makeText(
                        context,
                        if (enabled) "Switched to Dark Theme" else "Switched to Light Theme",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // 4. Security & Access Control
            SecurityCard(
                user = user,
                onBiometricToggle = { enabled ->
                    viewModel.toggleBiometric(enabled)
                    Toast.makeText(
                        context,
                        if (enabled) "Biometric quick login enabled!" else "Biometric login disabled.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onChangePinClick = { showChangePinDialog = true }
            )

            // 5. Kenyan Building Code & Compliance Quick Guide
            ComplianceGuideCard(
                onOpenFullGuide = { showComplianceDialog = true }
            )

            // 6. Interactive Architectural Tutorials
            TutorialsCard(
                onSelectTutorial = { selectedTutorialItem = it }
            )

            // 7. Help, Legal & App Info
            HelpAndLegalCard(
                onViewOnboarding = onViewOnboarding,
                onViewPrivacy = { showPrivacyDialog = true }
            )

            // 8. Account Session & Logout
            LogoutCard(
                onLogoutClick = { showLogoutConfirmDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun UserHeaderCard(
    user: UserEntity,
    onEditClick: () -> Unit,
    onAvatarColorChange: (String) -> Unit
) {
    val avatarColors = listOf("#00B4D8", "#4CAF50", "#FF9800", "#9C27B0", "#E91E63", "#2196F3")
    val avatarBgColor = remember(user.avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(user.avatarColorHex))
        } catch (e: Exception) {
            Color(0xFF00B4D8)
        }
    }

    val initials = remember(user.name) {
        user.name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "VA" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar Circle with Initials
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(avatarBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Member",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = user.role,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "County",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "County: ${user.county}, Kenya",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = user.phone,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Avatar theme color picker row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Badge Style:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(avatarColors) { colorHex ->
                        val colorVal = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(colorVal)
                                .border(
                                    width = if (user.avatarColorHex == colorHex) 2.dp else 0.dp,
                                    color = if (user.avatarColorHex == colorHex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onAvatarColorChange(colorHex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    blueprints: List<BlueprintEntity>,
    preferredCurrency: String,
    onExportData: () -> Unit
) {
    val totalArea = remember(blueprints) {
        blueprints.sumOf { it.totalAreaSqm.toDouble() }
    }

    val totalMinCostKes = remember(blueprints) {
        blueprints.sumOf { it.estimatedCostMinKes.toDouble() }
    }

    val currencySymbol = when (preferredCurrency) {
        "USD" -> "$"
        "EUR" -> "€"
        else -> "Ksh "
    }

    val formatCost = { costInKes: Double ->
        val converted = when (preferredCurrency) {
            "USD" -> costInKes / 130.0
            "EUR" -> costInKes / 140.0
            else -> costInKes
        }
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 0
        "$currencySymbol${formatter.format(converted)}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Local Architectural Portfolio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onExportData,
                    modifier = Modifier.testTag("export_portfolio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Export Summary",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Data", fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stat 1: Saved Floorplans
                StatBox(
                    label = "Saved Projects",
                    value = "${blueprints.size}",
                    icon = Icons.Default.MapsHomeWork,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Stat 2: Total Sqm
                StatBox(
                    label = "Total Plotted Area",
                    value = "${totalArea.toInt()} m²",
                    icon = Icons.Default.SquareFoot,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Stat 3: Total Portfolio Value
                StatBox(
                    label = "Portfolio Value",
                    value = formatCost(totalMinCostKes),
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesCard(
    user: UserEntity,
    onPreferenceChange: (currency: String, unit: String, preset: String) -> Unit
) {
    var selectedCurrency by remember(user.preferredCurrency) { mutableStateOf(user.preferredCurrency) }
    var selectedUnit by remember(user.preferredUnit) { mutableStateOf(user.preferredUnit) }
    var selectedPreset by remember(user.constructionPreset) { mutableStateOf(user.constructionPreset) }

    val currencyOptions = listOf("KES", "USD", "EUR")
    val unitOptions = listOf("Sqm", "Sqft")
    val presetOptions = listOf("Basic Standard", "Mid-Range Finishes", "Premium High-End", "Luxury Executive")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preferences_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Estimator & Calculation Preferences",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Currency Choice
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Preferred Currency Display", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencyOptions.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency == curr,
                            onClick = {
                                selectedCurrency = curr
                                onPreferenceChange(selectedCurrency, selectedUnit, selectedPreset)
                            },
                            label = { Text(curr, fontSize = 12.sp) },
                            modifier = Modifier.testTag("pref_currency_$curr")
                        )
                    }
                }
            }

            // Unit Choice
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Measurement Unit", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    unitOptions.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = {
                                selectedUnit = unit
                                onPreferenceChange(selectedCurrency, selectedUnit, selectedPreset)
                            },
                            label = { Text(if (unit == "Sqm") "Square Meters (m²)" else "Square Feet (ft²)", fontSize = 12.sp) },
                            modifier = Modifier.testTag("pref_unit_$unit")
                        )
                    }
                }
            }

            // Preset Choice
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Default Construction Standard", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetOptions) { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                onPreferenceChange(selectedCurrency, selectedUnit, selectedPreset)
                            },
                            label = { Text(preset, fontSize = 12.sp) },
                            modifier = Modifier.testTag("pref_preset_$preset")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityCard(
    user: UserEntity,
    onBiometricToggle: (Boolean) -> Unit,
    onChangePinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Security & PIN Protection",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Biometric switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fingerprint / Biometric Login",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Use device biometrics for rapid access.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = user.biometricEnabled,
                    onCheckedChange = onBiometricToggle,
                    modifier = Modifier.testTag("biometric_switch")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Change PIN Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Passcode PIN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "4-digit security code stored in local Room DB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedButton(
                    onClick = onChangePinClick,
                    modifier = Modifier.testTag("change_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change PIN", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ComplianceGuideCard(
    onOpenFullGuide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compliance_guide_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Kenya County Approval Guide",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onOpenFullGuide,
                    modifier = Modifier.testTag("open_compliance_guide_button")
                ) {
                    Text("View Checklist", fontSize = 11.sp)
                }
            }

            Text(
                text = "Key requirements before starting physical construction in Nairobi, Kiambu, Machakos & other counties:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onOpenFullGuide,
                    label = { Text("NCA Clearance", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp)) }
                )
                AssistChip(
                    onClick = onOpenFullGuide,
                    label = { Text("NEMA Report", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp)) }
                )
                AssistChip(
                    onClick = onOpenFullGuide,
                    label = { Text("County Stamp", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp)) }
                )
            }
        }
    }
}

@Composable
fun HelpAndLegalCard(
    onViewOnboarding: () -> Unit,
    onViewPrivacy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("help_legal_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Support & System Information",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            ListItem(
                headlineContent = { Text("Interactive App Walkthrough", fontSize = 13.sp) },
                supportingContent = { Text("Review generator steps and feature controls", fontSize = 11.sp) },
                leadingContent = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewOnboarding() }
                    .testTag("profile_walkthrough_item")
            )

            ListItem(
                headlineContent = { Text("Privacy Policy & Terms of Service", fontSize = 13.sp) },
                supportingContent = { Text("Local Room database data ownership policy", fontSize = 11.sp) },
                leadingContent = { Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewPrivacy() }
                    .testTag("profile_privacy_item")
            )
        }
    }
}

// --- DIALOGS ---

@Composable
fun EditProfileDialog(
    user: UserEntity,
    onSave: (name: String, role: String, phone: String, county: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var role by remember { mutableStateOf(user.role) }
    var phone by remember { mutableStateOf(user.phone) }
    var county by remember { mutableStateOf(user.county) }

    val kenyanCounties = listOf("Nairobi", "Kiambu", "Machakos", "Nakuru", "Mombasa", "Kajiado", "Uasin Gishu", "Kilifi", "Kisumu")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Architectural Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Title / Profession") },
                    placeholder = { Text("e.g. Architect, Civil Engineer, Homeowner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_role_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_phone_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Primary County", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(kenyanCounties) { c ->
                        FilterChip(
                            selected = county == c,
                            onClick = { county = c },
                            label = { Text(c, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, role, phone, county)
                },
                modifier = Modifier.testTag("save_profile_dialog_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChangePinDialog(
    onSave: (currentPin: String, newPin: String, callback: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Passcode PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                errorMsg?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { if (it.length <= 6) currentPin = it },
                    label = { Text("Current 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("current_pin_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin != confirmPin) {
                        errorMsg = "New PIN and Confirm PIN do not match."
                        return@Button
                    }
                    onSave(currentPin, newPin) { success, msg ->
                        if (!success) errorMsg = msg
                    }
                },
                modifier = Modifier.testTag("submit_change_pin_button")
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BuildingComplianceDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("County Approval & NCA Guide", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Registered Architect & Engineer Stamps",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Architectural drawings must be prepared by a BORAQS registered architect and structural drawings endorsed by an EBK registered engineer.",
                    fontSize = 12.sp
                )

                HorizontalDivider()

                Text(
                    text = "2. NEMA Environmental Impact Assessment (EIA)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Projects exceeding standard residential density require NEMA clearance to confirm waste water & environmental compliance.",
                    fontSize = 12.sp
                )

                HorizontalDivider()

                Text(
                    text = "3. County Development Control Clearance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Mandatory setback distances: 3 meters front road clearance, 1.5 meters side boundaries, minimum 60% plot coverage for urban residential zones.",
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got It") }
        }
    )
}

@Composable
fun PrivacyTermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy & Local Data Storage", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your architectural floorplans, cost estimates, and profile settings are stored strictly in your device's local Android Room database.",
                    fontSize = 12.sp
                )
                Text(
                    text = "No private project details or financial estimation notes leave your device unless explicitly exported via the PDF Exporter tool.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun LogoutCard(
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("logout_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Account Session & Logout",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = "Log out of your active session to protect your saved floorplans and local estimation database.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_page_logout_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out of Account", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- TUTORIAL MODELS & COMPOSABLES ---

data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val tip: String? = null
)

data class TutorialItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val steps: List<TutorialStep>
)

val builtInTutorials = listOf(
    TutorialItem(
        id = "tut_floorplans",
        title = "Designing & Customizing Floorplans",
        subtitle = "Learn how to tweak plot dimensions, room arrangements, and vector blueprint overlays.",
        category = "Architecture",
        icon = Icons.Default.HomeWork,
        steps = listOf(
            TutorialStep(
                stepNumber = 1,
                title = "Specify Plot & Building Dimensions",
                description = "Navigate to the Generator Setup screen. Input your width and depth in meters or feet. The generator ensures mandatory 3m front setback clearance.",
                tip = "Pro tip: Measure your plot boundaries with a surveyor tape before setting custom dimensions."
            ),
            TutorialStep(
                stepNumber = 2,
                title = "Choose Architectural Layout Style",
                description = "Select between Single-Story Bungalow, Executive Maisonette, Modern Flat, or Minimalist Cottage. The algorithm adjusts hallway flow and spatial efficiency.",
                tip = "Bungalows are ideal for suburban 50x100 plots, while Maisonettes double usable square meters."
            ),
            TutorialStep(
                stepNumber = 3,
                title = "Interactive Blueprint Canvas",
                description = "Tap 'Open 2D Blueprint Canvas' to view auto-rendered wall thicknesses, door swings, window openings, and room labels.",
                tip = "Zoom and pan on the interactive canvas to inspect door clearances and room proportions."
            )
        )
    ),
    TutorialItem(
        id = "tut_boq",
        title = "Cost Estimation & Material Quantities",
        subtitle = "Understand how Bill of Quantities (BOQ) calculates cement, steel, timber, and roofing costs.",
        category = "Finance & Budget",
        icon = Icons.Default.AccountBalance,
        steps = listOf(
            TutorialStep(
                stepNumber = 1,
                title = "Select Construction Quality Standard",
                description = "Choose between Basic Standard, Mid-Range Finishes, or Premium High-End. Rates update automatically (e.g. Ksh 32,000/m² to Ksh 55,000/m²).",
                tip = "Mid-Range finishes cover standard porcelain tiles, machine-cut stone, and iron sheet roofing."
            ),
            TutorialStep(
                stepNumber = 2,
                title = "Review Structural Sub-Breakdown",
                description = "Tap on any saved project to view Substructure (foundation), Superstructure (walls & columns), Roofing, Plumbing/Electrical, and Finishes.",
                tip = "Foundation and walling typically account for ~45% of total structural expenditure."
            ),
            TutorialStep(
                stepNumber = 3,
                title = "Toggle Currency & Unit Displays",
                description = "Go to Profile Settings > Estimator Preferences to switch between KES, USD, EUR, or convert square meters to square feet.",
                tip = "Updates apply instantaneously across all saved project cost breakdowns."
            )
        )
    ),
    TutorialItem(
        id = "tut_permits",
        title = "County Permits & NCA Approvals",
        subtitle = "Step-by-step guide to clearing Nairobi, Kiambu, and Machakos building regulatory approvals.",
        category = "Legal & Compliance",
        icon = Icons.Default.Gavel,
        steps = listOf(
            TutorialStep(
                stepNumber = 1,
                title = "BORAQS Registered Architect Endorsement",
                description = "Your generated blueprint serves as the initial architectural draft. Final county submissions require stamping by a licensed BORAQS architect.",
                tip = "Ensure your title deed copy and survey plan are updated before submission."
            ),
            TutorialStep(
                stepNumber = 2,
                title = "EBK Structural Engineer Calculation",
                description = "For two-story (Maisonette) or multi-family structures, an Engineers Board of Kenya (EBK) structural report is mandatory.",
                tip = "Structural drawings detail soil bearing capacity, footing sizes, and steel rebar specifications."
            ),
            TutorialStep(
                stepNumber = 3,
                title = "County Development Permit & NCA Registration",
                description = "Submit stamped plans through the county portal (e.g. e-Development Nairobi). Once approved, register the project site with the National Construction Authority (NCA).",
                tip = "NCA issues a site signboard that must be displayed at your construction gate."
            )
        )
    ),
    TutorialItem(
        id = "tut_pdf_export",
        title = "Generating & Exporting PDF Reports",
        subtitle = "Create clean, printable PDF blueprints complete with official stamps and cost summaries.",
        category = "Documentation",
        icon = Icons.Default.Description,
        steps = listOf(
            TutorialStep(
                stepNumber = 1,
                title = "Open Project Report Exporter",
                description = "From any project screen, tap 'Export PDF Report'. The exporter bundles the 2D floorplan drawing with complete financial breakdowns.",
                tip = "Choose whether to include or hide detailed unit material price lists."
            ),
            TutorialStep(
                stepNumber = 2,
                title = "Share via WhatsApp or Email",
                description = "Use Android's native share sheet to instantly send the PDF to fundis, hardware suppliers, bank loan officers, or county assessors.",
                tip = "PDF files are saved directly in your device Documents folder for offline printing."
            )
        )
    ),
    TutorialItem(
        id = "tut_security",
        title = "PIN Security & Offline Data Persistence",
        subtitle = "How your architectural plans are secured locally in Android Room database.",
        category = "Security",
        icon = Icons.Default.Lock,
        steps = listOf(
            TutorialStep(
                stepNumber = 1,
                title = "Local Android Room Database Storage",
                description = "All floorplans, cost estimates, and custom user preferences are saved strictly on your local device. No sensitive project data is leaked online.",
                tip = "Your project database is safely preserved across app restarts."
            ),
            TutorialStep(
                stepNumber = 2,
                title = "Configure 4-Digit Passcode & Biometrics",
                description = "In Profile Settings > Security, enable Fingerprint Login or set a 4-digit PIN code to lock access to confidential cost estimates.",
                tip = "You can change or reset your PIN anytime using your current passcode."
            )
        )
    )
)

@Composable
fun TutorialsCard(
    onSelectTutorial: (TutorialItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tutorials_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Architectural Tutorials & Guides",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${builtInTutorials.size} Guides",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Master floorplan generation, cost estimation, county approvals, and PDF exporting with these step-by-step guides:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                builtInTutorials.forEach { tutorial ->
                    TutorialListItem(
                        tutorial = tutorial,
                        onClick = { onSelectTutorial(tutorial) }
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialListItem(
    tutorial: TutorialItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tutorial_item_${tutorial.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tutorial.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tutorial.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = tutorial.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Read Tutorial",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun TutorialDetailDialog(
    tutorial: TutorialItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = tutorial.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = tutorial.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Category: ${tutorial.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = tutorial.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                tutorial.steps.forEach { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${step.stepNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = step.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = step.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        step.tip?.let { tip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tip,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_tutorial_dialog_button")
            ) {
                Text("Got It, Close Guide")
            }
        }
    )
}

@Composable
fun ThemeCard(
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Theme & Visual Appearance",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDarkMode) "Dark Theme Mode" else "Light Theme Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isDarkMode) "Optimized for low-light blueprint drafting" else "High-contrast daylight view for client presentations",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onToggleTheme,
                    thumbContent = {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.testTag("theme_toggle_switch")
                )
            }
        }
    }
}
