package com.example.ui.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class AudioNarratorManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    
    var isInitialized by mutableStateOf(false)
        private set
        
    var isSpeaking by mutableStateOf(false)
        private set

    var speechRate by mutableFloatStateOf(1.0f)
        private set

    var currentText by mutableStateOf("")
        private set

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking = false
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                tts?.setSpeechRate(speechRate)
            }
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) return
        currentText = text
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, queueMode, null, "ARCHITECTURAL_NARRATION_ID")
        isSpeaking = true
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun setSpeed(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isSpeaking = false
        isInitialized = false
    }
}

@Composable
fun rememberAudioNarrator(): AudioNarratorManager {
    val context = LocalContext.current
    val narrator = remember { AudioNarratorManager(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            narrator.release()
        }
    }
    return narrator
}

@Composable
fun AudioNarrationPlayerBar(
    narrator: AudioNarratorManager,
    title: String,
    narrationScript: String,
    modifier: Modifier = Modifier
) {
    var expandedSettings by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_narration_player_bar"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (narrator.isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (narrator.isSpeaking) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                            contentDescription = "Audio Walkthrough",
                            tint = if (narrator.isSpeaking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (narrator.isSpeaking) "Voice Narration Playing..." else "Tap play for Audio Tour",
                            fontSize = 11.sp,
                            color = if (narrator.isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Play / Stop Button
                    FilledIconButton(
                        onClick = {
                            if (narrator.isSpeaking) {
                                narrator.stop()
                            } else {
                                narrator.speak(narrationScript)
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("audio_play_stop_button")
                    ) {
                        Icon(
                            imageVector = if (narrator.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (narrator.isSpeaking) "Stop Narration" else "Play Voice Narration",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Speed Settings Button
                    IconButton(
                        onClick = { expandedSettings = !expandedSettings },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("audio_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Voice Speed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expandedSettings) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Speed: ${narrator.speechRate}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { rate ->
                            FilterChip(
                                selected = narrator.speechRate == rate,
                                onClick = { narrator.setSpeed(rate) },
                                label = { Text("${rate}x", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
