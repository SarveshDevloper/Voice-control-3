package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.SettingsAccessibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AwaazWebBrowserOverlay
import com.example.ui.screens.BilingualCommandGuideScreen
import com.example.ui.screens.CommanderDeckScreen
import com.example.ui.screens.CustomShortcutsScreen
import com.example.ui.screens.SystemSetupScreen
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AwaazViewModel

enum class AwaazTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    COMMANDER("Commander", Icons.Filled.Mic, Icons.Outlined.MicNone, "nav_tab_commander"),
    SHORTCUTS("Shortcuts", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tab_shortcuts"),
    GUIDE("Commands", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_tab_guide"),
    SETUP("Setup", Icons.Filled.SettingsAccessibility, Icons.Outlined.SettingsAccessibility, "nav_tab_setup")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AwaazControlApp()
            }
        }
    }
}

@Composable
fun AwaazControlApp(
    awaazViewModel: AwaazViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by awaazViewModel.uiState.collectAsStateWithLifecycle()
    val isAccessibilityConnected by awaazViewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val macros by awaazViewModel.macros.collectAsStateWithLifecycle()
    val commandLogs by awaazViewModel.commandLogs.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AwaazTab.COMMANDER) }

    // System Speech Dialog Fallback Launcher (if background SpeechRecognizer fails on a device)
    val systemSpeechDialogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val spokenList = activityResult.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val topPhrase = spokenList?.firstOrNull()?.trim().orEmpty()
            if (topPhrase.isNotEmpty()) {
                awaazViewModel.executeSpokenOrTypedCommand(topPhrase)
            }
        }
    }

    val launchSpeechWithFallback = {
        awaazViewModel.startVoiceListening(
            onFallbackToSystemDialog = {
                try {
                    systemSpeechDialogLauncher.launch(awaazViewModel.buildSystemVoiceRecognizerIntent())
                } catch (_: Exception) {
                }
            }
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        awaazViewModel.updateMicPermissionStatus(granted)
        if (granted) {
            launchSpeechWithFallback()
        }
    }

    val triggerMicOrRequestPermission = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (uiState.isListening) {
                awaazViewModel.stopVoiceListening()
            } else {
                launchSpeechWithFallback()
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        awaazViewModel.updateMicPermissionStatus(hasAudio)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasAudio = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                awaazViewModel.updateMicPermissionStatus(hasAudio)
                awaazViewModel.refreshTelemetry()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Back button closes built-in YouTube/Web player first, then returns to Commander tab
    if (uiState.activeWebUrl != null) {
        BackHandler {
            awaazViewModel.closeActiveWebPlayer()
        }
    } else if (currentTab != AwaazTab.COMMANDER) {
        BackHandler {
            currentTab = AwaazTab.COMMANDER
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepObsidian),
        containerColor = DeepObsidian,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AwaazTopHeader(
                isListening = uiState.isListening,
                isAccessibilityConnected = isAccessibilityConnected,
                onQuickMicClick = triggerMicOrRequestPermission
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(width = 1.dp, color = GlassBorder)
            ) {
                AwaazTab.entries.forEach { tab ->
                    val selected = currentTab == tab && uiState.activeWebUrl == null
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            awaazViewModel.closeActiveWebPlayer()
                            currentTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepObsidian,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AwaazTab.COMMANDER -> {
                    CommanderDeckScreen(
                        uiState = uiState,
                        isAccessibilityConnected = isAccessibilityConnected,
                        logs = commandLogs,
                        onMicTrigger = triggerMicOrRequestPermission,
                        onToggleContinuousRadar = { enable ->
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted || !enable) {
                                awaazViewModel.toggleContinuousRadar(enable)
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onToggleLanguage = { awaazViewModel.toggleLanguageTag() },
                        onToggleVoiceFeedback = { awaazViewModel.toggleVoiceFeedback() },
                        onExecutePhrase = { phrase -> awaazViewModel.executeSpokenOrTypedCommand(phrase) },
                        onExecuteActionCode = { code, label -> awaazViewModel.executeDirectActionCode(code, label) },
                        onClearLogs = { awaazViewModel.clearAllCommandLogs() },
                        onNavigateToSetup = { currentTab = AwaazTab.SETUP }
                    )
                }

                AwaazTab.SHORTCUTS -> {
                    CustomShortcutsScreen(
                        macros = macros,
                        onAddMacro = { trigger, code, desc, cat ->
                            awaazViewModel.addCustomMacro(trigger, code, desc, cat)
                        },
                        onDeleteMacro = { id -> awaazViewModel.deleteCustomMacro(id) },
                        onRunMacro = { macro ->
                            awaazViewModel.executeSpokenOrTypedCommand(macro.triggerPhrase)
                        }
                    )
                }

                AwaazTab.GUIDE -> {
                    BilingualCommandGuideScreen(
                        onTryPhrase = { phrase ->
                            awaazViewModel.executeSpokenOrTypedCommand(phrase)
                            currentTab = AwaazTab.COMMANDER
                        }
                    )
                }

                AwaazTab.SETUP -> {
                    SystemSetupScreen(
                        isAccessibilityConnected = isAccessibilityConnected,
                        hasMicPermission = uiState.hasMicPermission,
                        voiceFeedbackEnabled = uiState.voiceFeedbackEnabled,
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onToggleVoiceFeedback = { awaazViewModel.toggleVoiceFeedback() }
                    )
                }
            }

            // Built-in Fullscreen YouTube & Web Player Overlay (Guaranteed to open even if YouTube app is missing)
            val webUrl = uiState.activeWebUrl
            if (webUrl != null) {
                AwaazWebBrowserOverlay(
                    url = webUrl,
                    title = uiState.activeWebTitle,
                    isListening = uiState.isListening,
                    onMicClick = triggerMicOrRequestPermission,
                    onClose = { awaazViewModel.closeActiveWebPlayer() }
                )
            }

            // Interactive Voice Command Simulator Sheet (Automatically opens when Emulator Mic has 0% audio)
            if (uiState.showEmulatorVoicePadModal) {
                com.example.ui.components.EmulatorVoiceSimulatorSheet(
                    diagnosticStatusText = uiState.diagnosticStatusText,
                    onExecutePhrase = { phrase ->
                        awaazViewModel.executeSpokenOrTypedCommand(phrase)
                    },
                    onRetryHardwareMic = {
                        awaazViewModel.dismissEmulatorVoicePadModal()
                        awaazViewModel.runHardwareMicTest()
                    },
                    onDismiss = {
                        awaazViewModel.dismissEmulatorVoicePadModal()
                    }
                )
            }
        }
    }
}

@Composable
private fun AwaazTopHeader(
    isListening: Boolean,
    isAccessibilityConnected: Boolean,
    onQuickMicClick: () -> Unit
) {
    Surface(
        color = DeepObsidian,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isListening) NeonEmerald else ElectricCyan)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "VOICE CONTROL",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAccessibilityConnected) {
                            "Full Hands-Free Phone Commander • Active"
                        } else {
                            "Bilingual Voice Phone Controller (Hindi & English)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                onClick = onQuickMicClick,
                color = if (isListening) NeonEmerald else ElevatedNavy,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isListening) NeonEmerald else ElectricCyan.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50)
                    )
                    .testTag("top_bar_mic_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Mic",
                        tint = if (isListening) DeepObsidian else ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "Listening…" else "Boliye",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isListening) DeepObsidian else ElectricCyan
                    )
                }
            }
        }
    }
}
