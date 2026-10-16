package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.BlueprintViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: BlueprintViewModel,
    onLoginSuccess: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("homeowner@nyumbaplan.ke") }
    var pin by remember { mutableStateOf("2540") }
    var selectedRole by remember { mutableStateOf("Homeowner & Builder") }
    var selectedCounty by remember { mutableStateOf("Nairobi") }
    var pinVisible by remember { mutableStateOf(false) }

    // Dialog state for social login simulation
    var activeSocialModalProvider by remember { mutableStateOf<String?>(null) }
    var showBiometricModal by remember { mutableStateOf(false) }
    var isBiometricScanning by remember { mutableStateOf(false) }
    var biometricScanSuccess by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val rolesList = listOf("Homeowner & Builder", "Licensed Architect", "Quantity Surveyor", "General Contractor")
    val countiesList = listOf("Nairobi", "Kiambu", "Machakos", "Mombasa", "Nakuru", "Kisumu", "Kajiado", "Uasin Gishu")

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onLoginSuccess()
        }
    }

    // Pulse animation for blueprint accents
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D18))
    ) {
        // Architectural Schematic Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 40.dp.toPx()
            val gridColor = Color(0xFF00E5FF).copy(alpha = 0.04f)
            val subGridColor = Color(0xFF00E5FF).copy(alpha = 0.015f)

            // Grid lines
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = if ((x / gridStep).toInt() % 4 == 0) gridColor else subGridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridStep
            }

            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = if ((y / gridStep).toInt() % 4 == 0) gridColor else subGridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            // Decorative blueprint corner markers
            val cornerLen = 30.dp.toPx()
            val markerColor = Color(0xFF00E5FF).copy(alpha = 0.15f)
            // Top Left
            drawLine(markerColor, Offset(16.dp.toPx(), 16.dp.toPx()), Offset(16.dp.toPx() + cornerLen, 16.dp.toPx()), 2f)
            drawLine(markerColor, Offset(16.dp.toPx(), 16.dp.toPx()), Offset(16.dp.toPx(), 16.dp.toPx() + cornerLen), 2f)
            // Top Right
            drawLine(markerColor, Offset(size.width - 16.dp.toPx(), 16.dp.toPx()), Offset(size.width - 16.dp.toPx() - cornerLen, 16.dp.toPx()), 2f)
            drawLine(markerColor, Offset(size.width - 16.dp.toPx(), 16.dp.toPx()), Offset(size.width - 16.dp.toPx(), 16.dp.toPx() + cornerLen), 2f)
        }

        // Ambient radial light gradients
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.12f * pulseGlow), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.10f * pulseGlow), Color.Transparent)
                    )
                )
        )

        // Main Content Scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // --- App Brand Header ---
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00E5FF).copy(alpha = 0.2f), Color(0xFF3B82F6).copy(alpha = 0.15f))
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF3B82F6).copy(alpha = 0.4f))),
                        RoundedCornerShape(22.dp)
                    )
                    .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF00E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Security Logo",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "VAPHBG",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif
            )

            Text(
                text = "Video & Photo House Blueprint Generator",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            // AI Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00E5FF), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI Spatial Architecture Engine • Kenya BCA",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Glassmorphic Auth Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0D1726).copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Segmented Pill Tab Bar (Sign In / Register)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFF080F1D), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (!isRegisterMode) Color(0xFF00E5FF) else Color.Transparent
                                )
                                .clickable {
                                    isRegisterMode = false
                                    viewModel.logout()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                color = if (!isRegisterMode) Color(0xFF050B14) else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (isRegisterMode) Color(0xFF00E5FF) else Color.Transparent
                                )
                                .clickable {
                                    isRegisterMode = true
                                    viewModel.logout()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                color = if (isRegisterMode) Color(0xFF050B14) else Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Mode Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isRegisterMode) Icons.Default.Person else Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRegisterMode) "Register Contractor / Homeowner" else "Authorized Access Login",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Registration Specific Fields
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name / Company", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "Name", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF334155),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedContainerColor = Color(0xFF080F1D),
                                unfocusedContainerColor = Color(0xFF080F1D)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Role Selector Chips
                        Text(
                            text = "Professional Role",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rolesList.take(2).forEach { roleItem ->
                                val isSelected = selectedRole == roleItem
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF080F1D),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedRole = roleItem }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = roleItem,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedContainerColor = Color(0xFF080F1D),
                            unfocusedContainerColor = Color(0xFF080F1D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Security PIN Input
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.take(6) },
                        label = { Text("Security PIN (4-6 digits)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                if (isRegisterMode) {
                                    viewModel.registerUser(name, email, pin)
                                } else {
                                    viewModel.loginUser(email, pin)
                                }
                            }
                        ),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "PIN", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                Icon(
                                    imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (pinVisible) "Hide PIN" else "Show PIN",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF334155),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedContainerColor = Color(0xFF080F1D),
                            unfocusedContainerColor = Color(0xFF080F1D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_pin_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Error message banner
                    loginError?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Main Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (isRegisterMode) {
                                viewModel.registerUser(name, email, pin)
                            } else {
                                viewModel.loginUser(email, pin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF050B14)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0xFF00E5FF))
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isRegisterMode) Icons.Default.Person else Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRegisterMode) "Create Account & Unlock" else "Unlock VAPHBG Studio",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Switch Auth Mode text button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                isRegisterMode = !isRegisterMode
                            },
                            modifier = Modifier.testTag("toggle_auth_mode_button")
                        ) {
                            Text(
                                text = if (isRegisterMode) "Already have an account? Sign In" else "New to VAPHBG? Create account",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Social Logins Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E293B))
                Text(
                    text = "  OR INSTANT SOCIAL LOGIN  ",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E293B))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Social Buttons Grid (Google and Apple)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google Button
                SocialLoginCard(
                    title = "Continue with Google",
                    tag = "social_login_google",
                    accentColor = Color(0xFF4285F4),
                    modifier = Modifier.weight(1f),
                    icon = { GoogleIconBadge() },
                    onClick = {
                        activeSocialModalProvider = "Google"
                    }
                )

                // Apple Button
                SocialLoginCard(
                    title = "Sign in with Apple",
                    tag = "social_login_apple",
                    accentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    icon = { AppleIconBadge() },
                    onClick = {
                        activeSocialModalProvider = "Apple"
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Quick Demo Profiles (Quick Fill Chips) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A1220), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Demo Account Presets",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DemoAccountChip(
                        roleLabel = "🏡 Homeowner",
                        emailValue = "homeowner@nyumbaplan.ke",
                        tag = "demo_chip_homeowner",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            email = "homeowner@nyumbaplan.ke"
                            pin = "2540"
                            viewModel.registerUser("Standard Homeowner", email, pin)
                        }
                    )
                    DemoAccountChip(
                        roleLabel = "📐 Architect",
                        emailValue = "arch.nairobi@nyumbaplan.ke",
                        tag = "demo_chip_architect",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            email = "arch.nairobi@nyumbaplan.ke"
                            pin = "2540"
                            viewModel.loginWithSocialProvider(
                                providerName = "NCA Direct",
                                email = "arch.nairobi@nyumbaplan.ke",
                                name = "Arch. Dennis Karanja (BORAQS #A1082)",
                                role = "Licensed Architect & QS",
                                county = "Nairobi"
                            )
                        }
                    )
                    DemoAccountChip(
                        roleLabel = "🏗️ Contractor",
                        emailValue = "contractor@nyumbaplan.ke",
                        tag = "demo_chip_contractor",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            email = "contractor@nyumbaplan.ke"
                            pin = "2540"
                            viewModel.loginWithSocialProvider(
                                providerName = "Contractor Direct",
                                email = "contractor@nyumbaplan.ke",
                                name = "Eng. Moses Otieno (NCA 1)",
                                role = "General Contractor",
                                county = "Mombasa"
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Biometric Passkey Unlock Bar ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        showBiometricModal = true
                        isBiometricScanning = true
                        biometricScanSuccess = false
                        coroutineScope.launch {
                            delay(1200)
                            isBiometricScanning = false
                            biometricScanSuccess = true
                            delay(600)
                            showBiometricModal = false
                            viewModel.loginWithBiometrics()
                        }
                    }
                    .testTag("biometric_simulation_button"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1D33)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Scan",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quick Unlock with Biometrics & Passkey",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Touch sensor or look at camera for instant hardware bypass",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security compliance footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AES-256 On-Device SQLite Storage • Offline First",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // --- Social Login Simulation Modal ---
    activeSocialModalProvider?.let { provider ->
        SocialLoginDialog(
            providerName = provider,
            onDismiss = { activeSocialModalProvider = null },
            onAccountSelected = { emailChosen, nameChosen, roleChosen ->
                activeSocialModalProvider = null
                viewModel.loginWithSocialProvider(
                    providerName = provider,
                    email = emailChosen,
                    name = nameChosen,
                    role = roleChosen
                )
            }
        )
    }

    // --- Biometric Authentication Simulated Dialog ---
    if (showBiometricModal) {
        Dialog(onDismissRequest = { showBiometricModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2C)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                if (biometricScanSuccess) Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color(0xFF00E5FF).copy(alpha = 0.15f),
                                CircleShape
                            )
                            .border(
                                2.dp,
                                if (biometricScanSuccess) Color(0xFF10B981) else Color(0xFF00E5FF),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (biometricScanSuccess) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Scanning",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (biometricScanSuccess) "Identity Verified!" else "Verifying Hardware Biometrics...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (biometricScanSuccess) "Unlocking VAPHBG Workspace" else "Please touch fingerprint sensor or align face",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    if (!biometricScanSuccess) {
                        Spacer(modifier = Modifier.height(18.dp))
                        CircularProgressIndicator(
                            color = Color(0xFF00E5FF),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Social Login Card Composable ---
@Composable
private fun SocialLoginCard(
    title: String,
    tag: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A2C)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

// --- Demo Account Quick Chip ---
@Composable
private fun DemoAccountChip(
    roleLabel: String,
    emailValue: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF142036))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = roleLabel,
            color = Color(0xFF00E5FF),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// --- Social Provider Logos ---
@Composable
private fun GoogleIconBadge() {
    Canvas(modifier = Modifier.size(18.dp)) {
        // Draw standard stylized Google 'G' geometry
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.2f
        val stroke = 3.dp.toPx()

        // Google Blue arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Google Green arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Google Yellow arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Google Red arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = stroke)
        )
        // Horizontal bar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(size.width - 2.dp.toPx(), center.y),
            strokeWidth = stroke
        )
    }
}

@Composable
private fun AppleIconBadge() {
    // Elegant apple silhouette representation
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF0E1A2C), CircleShape)
                .offset(x = 4.dp, y = (-2).dp)
        )
    }
}

// --- Social Login Simulation Dialog ---
@Composable
private fun SocialLoginDialog(
    providerName: String,
    onDismiss: () -> Unit,
    onAccountSelected: (email: String, name: String, role: String) -> Unit
) {
    val sampleAccounts = when (providerName) {
        "Google" -> listOf(
            Triple("zestful.tech@outlook.com", "Zestful Architect (Google SSO)", "Licensed Architect"),
            Triple("homeowner.kenya@gmail.com", "Brian Ochieng (Google)", "Homeowner & Builder")
        )
        else -> listOf(
            Triple("architect.apple@privaterelay.appleid.com", "Verified Apple ID User", "Licensed Architect"),
            Triple("developer.nairobi@icloud.com", "Grace Wanjiku (Apple ID)", "Property Developer")
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2D)),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign In with $providerName",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select account to authorize VAPHBG",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "AVAILABLE IDENTITIES",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                sampleAccounts.forEach { (accEmail, accName, accRole) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onAccountSelected(accEmail, accName, accRole)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16233B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = accName.take(1),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = accName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = accEmail,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Role: $accRole",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        }
    }
}
