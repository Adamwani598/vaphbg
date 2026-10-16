package com.example.ui.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

enum class VoiceCommandAction(val label: String, val description: String) {
    START_SCAN("Start Scanning", "Generates floorplan layout via scanner / AI engine"),
    CALCULATE_COSTS("Calculate Costs", "Navigates to construction cost estimation breakdown"),
    SAVE_PROJECT("Save Project", "Saves layout & exports compliance documentation"),
    VIEW_3D("3D Studio", "Switches to 3D volumetric model view"),
    VIEW_2D("2D Floor Plan", "Switches to 2D blueprint canvas view"),
    SHOW_HELP("Voice Help", "Opens available voice commands list")
}

class VoiceCommandManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null

    var isListening by mutableStateOf(false)
        private set

    var lastSpokenText by mutableStateOf("")
        private set

    var feedbackMessage by mutableStateOf<String?>(null)
        private set

    var onCommandRecognized: ((VoiceCommandAction) -> Unit)? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        feedbackMessage = "Listening... Speak command"
                    }

                    override fun onBeginningOfSpeech() {
                        feedbackMessage = "Processing speech..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No command matched. Try 'Start Scanning'"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap mic again"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            else -> "Mic listening paused"
                        }
                        feedbackMessage = errorMsg
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            lastSpokenText = recognized
                            processSpokenText(recognized)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            lastSpokenText = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command: 'Start Scanning', 'Calculate Costs', 'Save Project'")
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                feedbackMessage = "Listening for hands-free voice command..."
            } catch (e: Exception) {
                isListening = false
                feedbackMessage = "Mic error: ${e.localizedMessage}"
            }
        } else {
            feedbackMessage = "Speech Recognition not available on this device"
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    fun processSpokenText(text: String) {
        val lower = text.lowercase(Locale.getDefault())
        val action = when {
            lower.contains("scan") || lower.contains("start scanning") || lower.contains("generate") || lower.contains("create") -> VoiceCommandAction.START_SCAN
            lower.contains("cost") || lower.contains("calculate") || lower.contains("estimate") || lower.contains("budget") -> VoiceCommandAction.CALCULATE_COSTS
            lower.contains("save") || lower.contains("export") || lower.contains("report") -> VoiceCommandAction.SAVE_PROJECT
            lower.contains("3d") || lower.contains("three d") || lower.contains("volumetric") -> VoiceCommandAction.VIEW_3D
            lower.contains("2d") || lower.contains("floor plan") || lower.contains("blueprint") || lower.contains("canvas") -> VoiceCommandAction.VIEW_2D
            lower.contains("help") || lower.contains("command") -> VoiceCommandAction.SHOW_HELP
            else -> null
        }

        if (action != null) {
            feedbackMessage = "Executed Voice Command: ${action.label}"
            onCommandRecognized?.invoke(action)
        } else {
            feedbackMessage = "Unrecognized command: \"$text\". Tap help for options."
        }
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

@Composable
fun rememberVoiceCommandManager(
    onCommand: (VoiceCommandAction) -> Unit
): VoiceCommandManager {
    val context = LocalContext.current
    val manager = remember { VoiceCommandManager(context) }

    DisposableEffect(Unit) {
        manager.onCommandRecognized = onCommand
        onDispose {
            manager.release()
        }
    }
    return manager
}

@Composable
fun VoiceCommandFab(
    manager: VoiceCommandManager,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = if (manager.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        contentColor = if (manager.isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = modifier
            .size(48.dp)
            .scale(if (manager.isListening) pulseScale else 1f)
            .testTag("voice_command_mic_fab")
    ) {
        Icon(
            imageVector = if (manager.isListening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (manager.isListening) "Stop Mic Listening" else "Start Hands-Free Voice Command",
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Modern, non-intrusive Voice Command Modal Sheet that appears only when active or requested.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandBottomSheet(
    manager: VoiceCommandManager,
    onDismiss: () -> Unit,
    onExecuteAction: (VoiceCommandAction) -> Unit
) {
    val context = LocalContext.current
    var showHelpDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            manager.startListening()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = if (manager.isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (manager.isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (manager.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (manager.isListening) "Listening for Command..." else "Hands-Free Voice Assistant",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (manager.isListening) "Speak naturally into microphone" else "Tap microphone to speak",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.testTag("voice_help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Transcript / Status Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            manager.lastSpokenText.isNotEmpty() -> "\"${manager.lastSpokenText}\""
                            manager.isListening -> "Listening..."
                            else -> "Try saying: \"Calculate Costs\", \"3D Studio\", or \"Start Scanning\""
                        },
                        fontSize = 13.sp,
                        fontWeight = if (manager.lastSpokenText.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    manager.feedbackMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Big Mic Action Button
            FloatingActionButton(
                onClick = {
                    if (manager.isListening) {
                        manager.stopListening()
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPerm) {
                            manager.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                containerColor = if (manager.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (manager.isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("voice_sheet_mic_button")
            ) {
                Icon(
                    imageVector = if (manager.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Mic",
                    modifier = Modifier.size(30.dp)
                )
            }

            // Quick Direct Action Shortcut Chips
            Text(
                text = "Or tap a quick action:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                items(VoiceCommandAction.values()) { cmd ->
                    AssistChip(
                        onClick = {
                            manager.processSpokenText(cmd.label)
                            onExecuteAction(cmd)
                            onDismiss()
                        },
                        label = { Text(cmd.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (cmd) {
                                    VoiceCommandAction.START_SCAN -> Icons.Default.Camera
                                    VoiceCommandAction.CALCULATE_COSTS -> Icons.Default.Calculate
                                    VoiceCommandAction.SAVE_PROJECT -> Icons.Default.Save
                                    VoiceCommandAction.VIEW_3D -> Icons.Default.HomeWork
                                    VoiceCommandAction.VIEW_2D -> Icons.Default.Map
                                    VoiceCommandAction.SHOW_HELP -> Icons.Default.Help
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("voice_chip_${cmd.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showHelpDialog) {
        VoiceCommandHelpDialog(
            onDismiss = { showHelpDialog = false },
            onSelectCommand = { cmd ->
                showHelpDialog = false
                manager.processSpokenText(cmd.label)
                onExecuteAction(cmd)
                onDismiss()
            }
        )
    }
}

/**
 * Backward compatibility wrapper that provides the VoiceCommandFab + bottom sheet
 */
@Composable
fun VoiceCommandHUD(
    manager: VoiceCommandManager,
    modifier: Modifier = Modifier,
    onExecuteAction: (VoiceCommandAction) -> Unit
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            manager.startListening()
            showSheet = true
        }
    }

    // Auto show sheet if listening begins externally
    LaunchedEffect(manager.isListening) {
        if (manager.isListening) {
            showSheet = true
        }
    }

    VoiceCommandFab(
        manager = manager,
        modifier = modifier,
        onClick = {
            if (manager.isListening) {
                manager.stopListening()
            } else {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPerm) {
                    manager.startListening()
                    showSheet = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    )

    if (showSheet) {
        VoiceCommandBottomSheet(
            manager = manager,
            onDismiss = {
                showSheet = false
                if (manager.isListening) manager.stopListening()
            },
            onExecuteAction = onExecuteAction
        )
    }
}

@Composable
fun VoiceCommandHelpDialog(
    onDismiss: () -> Unit,
    onSelectCommand: (VoiceCommandAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Hands-Free Voice Commands", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Speak any of the following phrases into your microphone to control the 2D floorplan generation and project workflow hands-free:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                VoiceCommandAction.values().forEach { cmd ->
                    Surface(
                        onClick = { onSelectCommand(cmd) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (cmd) {
                                    VoiceCommandAction.START_SCAN -> Icons.Default.PlayArrow
                                    VoiceCommandAction.CALCULATE_COSTS -> Icons.Default.Calculate
                                    VoiceCommandAction.SAVE_PROJECT -> Icons.Default.Save
                                    VoiceCommandAction.VIEW_3D -> Icons.Default.HomeWork
                                    VoiceCommandAction.VIEW_2D -> Icons.Default.Map
                                    VoiceCommandAction.SHOW_HELP -> Icons.Default.Help
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\"${cmd.label}\"", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(cmd.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
