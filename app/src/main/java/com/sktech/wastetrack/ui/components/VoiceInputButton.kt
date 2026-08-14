package com.sktech.wastetrack.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sktech.wastetrack.ui.theme.SafetyOrange

enum class RegionalLanguage(val displayName: String, val localeTag: String) {
    ENGLISH("English (IN)", "en-IN"),
    HINDI("हिंदी (Hindi)", "hi-IN"),
    MARATHI("मराठी (Marathi)", "mr-IN")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(RegionalLanguage.ENGLISH) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission required for voice logging", Toast.LENGTH_SHORT).show()
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }
                    override fun onError(error: Int) {
                        Log.e("VoiceInput", "SpeechRecognizer error code: $error")
                        isListening = false
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        // Language Selector Chip
        FilterChip(
            selected = false,
            onClick = { showLanguageMenu = true },
            label = {
                Text(
                    when (selectedLanguage) {
                        RegionalLanguage.ENGLISH -> "EN"
                        RegionalLanguage.HINDI -> "HI"
                        RegionalLanguage.MARATHI -> "MR"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Language, contentDescription = "Language", modifier = Modifier.size(14.dp))
            }
        )

        DropdownMenu(
            expanded = showLanguageMenu,
            onDismissRequest = { showLanguageMenu = false }
        ) {
            RegionalLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName) },
                    onClick = {
                        selectedLanguage = lang
                        showLanguageMenu = false
                    }
                )
            }
        }

        // Voice Input Action Button
        FloatingActionButton(
            onClick = {
                if (!hasAudioPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@FloatingActionButton
                }

                if (speechRecognizer == null) {
                    Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                    return@FloatingActionButton
                }

                if (isListening) {
                    speechRecognizer.stopListening()
                    isListening = false
                } else {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage.localeTag)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLanguage.localeTag)
                        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, selectedLanguage.localeTag)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    }
                    speechRecognizer.startListening(intent)
                    isListening = true
                }
            },
            containerColor = if (isListening) SafetyOrange else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isListening) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Voice Input",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
