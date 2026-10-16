package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.BlueprintEntity
import com.example.data.BlueprintRepository
import com.example.ui.BlueprintViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.audio.VoiceCommandAction
import com.example.ui.audio.VoiceCommandHUD
import com.example.ui.audio.rememberVoiceCommandManager
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BlueprintViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database & Repositories
        val database = AppDatabase.getDatabase(this)
        val repository = BlueprintRepository(database)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[BlueprintViewModel::class.java]

        enableEdgeToEdge()

        val prefs = getSharedPreferences("nyumbaplan_prefs", MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaphbgApp(
                        viewModel = viewModel,
                        onboardingCompletedInitially = onboardingCompleted,
                        onOnboardingComplete = {
                            prefs.edit().putBoolean("onboarding_completed", true).apply()
                        }
                    )
                }
            }
        }
    }
}

sealed class AppScreen {
    object Onboarding : AppScreen()
    object Login : AppScreen()
    object Dashboard : AppScreen()
    object GeneratorSetup : AppScreen()
    object ActiveBlueprintDetails : AppScreen()
    object Profile : AppScreen()
    object ThreeDStudio : AppScreen()
    object MediaGallery : AppScreen()
    data class Camera(val mode: CameraCaptureMode = CameraCaptureMode.SKETCH) : AppScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaphbgApp(
    viewModel: BlueprintViewModel,
    onboardingCompletedInitially: Boolean,
    onOnboardingComplete: () -> Unit
) {
    var currentScreen by remember { 
        mutableStateOf<AppScreen>(
            if (onboardingCompletedInitially) AppScreen.Login else AppScreen.Onboarding
        ) 
    }
    val currentUser by viewModel.currentUser.collectAsState()
    val activeBlueprint by viewModel.activeBlueprint.collectAsState()

    // Handle session auto-routing
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            if (currentScreen != AppScreen.Onboarding) {
                currentScreen = AppScreen.Login
            }
        } else if (currentScreen == AppScreen.Login || currentScreen == AppScreen.Onboarding) {
            currentScreen = AppScreen.Dashboard
        }
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            AppScreen.Onboarding -> {
                OnboardingScreen(
                    onOnboardingComplete = {
                        onOnboardingComplete()
                        currentScreen = AppScreen.Login
                    }
                )
            }

            AppScreen.Login -> {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }

            AppScreen.Dashboard -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onCreateNewBlueprint = {
                        currentScreen = AppScreen.GeneratorSetup
                    },
                    onSelectBlueprint = { blueprint ->
                        viewModel.loadBlueprint(blueprint)
                        currentScreen = AppScreen.ActiveBlueprintDetails
                    },
                    onViewOnboarding = {
                        currentScreen = AppScreen.Onboarding
                    },
                    onOpenProfile = {
                        currentScreen = AppScreen.Profile
                    },
                    onOpenLiveCamera = { mode ->
                        currentScreen = AppScreen.Camera(mode)
                    },
                    onOpenMediaGallery = {
                        currentScreen = AppScreen.MediaGallery
                    }
                )
            }

            AppScreen.MediaGallery -> {
                MediaGalleryScreen(
                    viewModel = viewModel,
                    onOpenLiveCamera = { mode ->
                        currentScreen = AppScreen.Camera(mode)
                    },
                    onBlueprintGenerated = {
                        currentScreen = AppScreen.ActiveBlueprintDetails
                    },
                    onBack = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }

            AppScreen.Profile -> {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onLogout = {
                        currentScreen = AppScreen.Login
                    },
                    onViewOnboarding = {
                        currentScreen = AppScreen.Onboarding
                    }
                )
            }

            AppScreen.GeneratorSetup -> {
                GeneratorSetupScreen(
                    viewModel = viewModel,
                    onBlueprintGenerated = {
                        currentScreen = AppScreen.ActiveBlueprintDetails
                    },
                    onBack = {
                        currentScreen = AppScreen.Dashboard
                    },
                    onOpenLiveCamera = { mode ->
                        currentScreen = AppScreen.Camera(mode)
                    },
                    onOpenMediaGallery = {
                        currentScreen = AppScreen.MediaGallery
                    }
                )
            }

            is AppScreen.Camera -> {
                CameraScreen(
                    viewModel = viewModel,
                    initialMode = screen.mode,
                    onBlueprintGenerated = {
                        currentScreen = AppScreen.ActiveBlueprintDetails
                    },
                    onOpenMediaGallery = {
                        currentScreen = AppScreen.MediaGallery
                    },
                    onBack = {
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }

            AppScreen.ActiveBlueprintDetails -> {
                activeBlueprint?.let { blueprint ->
                    ActiveBlueprintTabsLayout(
                        blueprint = blueprint,
                        viewModel = viewModel,
                        onBack = {
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.Dashboard
                }
            }

            AppScreen.ThreeDStudio -> {
                activeBlueprint?.let { blueprint ->
                    ThreeDModelingScreen(
                        roomsJson = blueprint.roomsJson,
                        title = blueprint.title,
                        totalAreaSqm = blueprint.totalAreaSqm,
                        onBack = {
                            currentScreen = AppScreen.ActiveBlueprintDetails
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.Dashboard
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveBlueprintTabsLayout(
    blueprint: BlueprintEntity,
    viewModel: BlueprintViewModel,
    onBack: () -> Unit
) {
    var selectedDetailsTab by remember { mutableStateOf(0) } // 0: Draw Canvas, 1: Cost Estimation, 2: Technical Specifications
    val costBreakdown by viewModel.costBreakdown.collectAsState()

    val context = LocalContext.current

    val voiceManager = rememberVoiceCommandManager { action ->
        when (action) {
            VoiceCommandAction.START_SCAN -> {
                onBack()
            }
            VoiceCommandAction.CALCULATE_COSTS -> {
                selectedDetailsTab = 1
            }
            VoiceCommandAction.SAVE_PROJECT -> {
                viewModel.saveCurrentProgress(
                    status = "Saved",
                    onComplete = {
                        android.widget.Toast.makeText(context, "Project progress saved to local database!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                selectedDetailsTab = 2
            }
            VoiceCommandAction.VIEW_3D -> {
                selectedDetailsTab = 3
            }
            VoiceCommandAction.VIEW_2D -> {
                selectedDetailsTab = 0
            }
            VoiceCommandAction.SHOW_HELP -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = blueprint.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Status: ${blueprint.status} • Area: ${blueprint.totalAreaSqm} sqm",
                            fontSize = 10.sp,
                            color = Color(0xFF00B4D8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_to_dashboard_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            voiceManager.startListening()
                        },
                        modifier = Modifier.testTag("top_bar_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Assistant",
                            tint = if (voiceManager.isListening) Color(0xFFFF5252) else Color(0xFF00B4D8)
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.saveCurrentProgress(
                                status = "In Progress",
                                onComplete = {
                                    android.widget.Toast.makeText(context, "Project progress saved to local database!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.testTag("top_bar_save_progress_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Progress to Room DB",
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    IconButton(
                        onClick = {
                            PdfExporter.generatePdfAndShare(context, blueprint, true)
                        },
                        modifier = Modifier.testTag("top_bar_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Report",
                            tint = Color(0xFF00B4D8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1B2A))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1B263B),
                contentColor = Color(0xFF00B4D8)
            ) {
                NavigationBarItem(
                    selected = selectedDetailsTab == 0,
                    onClick = { selectedDetailsTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Blueprint Canvas") },
                    label = { Text("2D Blueprint", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF00B4D8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("tab_button_2d_blueprint")
                )

                NavigationBarItem(
                    selected = selectedDetailsTab == 1,
                    onClick = { selectedDetailsTab = 1 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Costings") },
                    label = { Text("NCA Costings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF00B4D8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("tab_button_nca_costings")
                )

                NavigationBarItem(
                    selected = selectedDetailsTab == 2,
                    onClick = { selectedDetailsTab = 2 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Specs") },
                    label = { Text("Specifications", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF00B4D8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("tab_button_specifications")
                )

                NavigationBarItem(
                    selected = selectedDetailsTab == 3,
                    onClick = { selectedDetailsTab = 3 },
                    icon = { Icon(Icons.Default.HomeWork, contentDescription = "3D Studio") },
                    label = { Text("3D Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF00B4D8),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("tab_button_3d_studio")
                )
            }
        },
        containerColor = Color(0xFF0D1B2A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedDetailsTab) {
                0 -> {
                    BlueprintCanvasScreen(
                        roomsJson = blueprint.roomsJson,
                        title = blueprint.title,
                        areaSqm = blueprint.totalAreaSqm,
                        onSaveRooms = { updatedRooms ->
                            viewModel.updateBlueprintRooms(blueprint.id, updatedRooms)
                        }
                    )
                }

                1 -> {
                    costBreakdown?.let { cb ->
                        CostBreakdownScreen(
                            breakdown = cb,
                            totalAreaSqm = blueprint.totalAreaSqm,
                            blueprint = blueprint
                        )
                    } ?: run {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00B4D8))
                        }
                    }
                }

                2 -> {
                    ReportExportScreen(blueprint = blueprint)
                }

                3 -> {
                    ThreeDModelingScreen(
                        roomsJson = blueprint.roomsJson,
                        title = blueprint.title,
                        totalAreaSqm = blueprint.totalAreaSqm,
                        onBack = null
                    )
                }
            }

            // Hands-Free Voice Command Floating Controller
            VoiceCommandHUD(
                manager = voiceManager,
                onExecuteAction = { action ->
                    when (action) {
                        VoiceCommandAction.START_SCAN -> onBack()
                        VoiceCommandAction.CALCULATE_COSTS -> selectedDetailsTab = 1
                        VoiceCommandAction.SAVE_PROJECT -> {
                            viewModel.saveCurrentProgress(status = "Saved")
                            selectedDetailsTab = 2
                        }
                        VoiceCommandAction.VIEW_3D -> selectedDetailsTab = 3
                        VoiceCommandAction.VIEW_2D -> selectedDetailsTab = 0
                        VoiceCommandAction.SHOW_HELP -> {}
                    }
                },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
            )
        }
    }
}
