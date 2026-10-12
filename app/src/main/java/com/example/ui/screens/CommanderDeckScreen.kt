package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CommandLogEntity
import com.example.ui.components.LiveSystemTelemetryRibbon
import com.example.ui.components.SonicVoiceVisualizerOrb
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import com.example.viewmodel.AwaazUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeckActionButton(
    val actionCode: String,
    val labelHi: String,
    val labelEn: String,
    val voicePhrase: String,
    val icon: ImageVector,
    val accent: Color
)

private val QUICK_VOICE_CHIPS = listOf(
    "Open YouTube",
    "Torch chalu karo",
    "Torch band karo",
    "Battery kitni hai",
    "Neeche scroll karo",
    "Upar scroll karo",
    "Home jao",
    "Peeche jao",
    "Aawaz badhao",
    "Aawaz mute karo",
    "Camera kholo",
    "Wi-Fi kholo",
    "Kitne baje hain"
)

private val SYSTEM_DECK_BUTTONS = listOf(
    DeckActionButton("OPEN_YOUTUBE", "YouTube Kholo", "Open YouTube", "Open YouTube", Icons.Filled.PlayArrow, CoralRed),
    DeckActionButton("TORCH_TOGGLE", "Torch ON/OFF", "Flashlight", "Torch chalu karo", Icons.Filled.FlashlightOn, WarmAmber),
    DeckActionButton("GLOBAL_SCROLL_DOWN", "Neeche Scroll", "Scroll Down", "Neeche scroll karo", Icons.Filled.KeyboardArrowDown, ElectricCyan),
    DeckActionButton("GLOBAL_SCROLL_UP", "Upar Scroll", "Scroll Up", "Upar scroll karo", Icons.Filled.KeyboardArrowUp, ElectricCyan),
    DeckActionButton("GLOBAL_BACK", "Peeche Jao", "Navigate Back", "Peeche jao", Icons.AutoMirrored.Filled.ArrowBack, NeonEmerald),
    DeckActionButton("GLOBAL_HOME", "Home Screen", "Go Home", "Home jao", Icons.Filled.Home, NeonEmerald),
    DeckActionButton("GLOBAL_RECENTS", "Recent Apps", "App Switcher", "Recent apps dikhao", Icons.Filled.ViewCarousel, NeonEmerald),
    DeckActionButton("GLOBAL_NOTIFICATIONS", "Notifications", "Open Alerts", "Notification dikhao", Icons.Filled.Notifications, ElectricCyan),
    DeckActionButton("GLOBAL_SCREENSHOT", "Screenshot", "Capture Screen", "Screenshot lo", Icons.Filled.Screenshot, WarmAmber),
    DeckActionButton("VOLUME_UP", "Aawaz +", "Volume Up", "Aawaz badhao", Icons.AutoMirrored.Filled.VolumeUp, ElectricCyan),
    DeckActionButton("VOLUME_MUTE", "Mute Phone", "Silence Audio", "Aawaz mute karo", Icons.AutoMirrored.Filled.VolumeOff, WarmAmber),
    DeckActionButton("BATTERY_STATUS", "Battery Info", "Speak Battery", "Battery kitni hai", Icons.Filled.BatteryChargingFull, NeonEmerald),
    DeckActionButton("TIME_READOUT", "Bolo Samay", "Speak Time", "Kitne baje hain", Icons.Filled.AccessTime, NeonEmerald),
    DeckActionButton("OPEN_CAMERA", "Camera Kholo", "Open Camera", "Camera kholo", Icons.Filled.CameraAlt, ElectricCyan),
    DeckActionButton("OPEN_DIALER", "Phone Dialer", "Make a Call", "Phone lagao", Icons.Filled.Phone, NeonEmerald),
    DeckActionButton("OPEN_WIFI", "Wi-Fi Panel", "Network Settings", "Wi-Fi kholo", Icons.Filled.Wifi, ElectricCyan)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommanderDeckScreen(
    uiState: AwaazUiState,
    isAccessibilityConnected: Boolean,
    logs: List<CommandLogEntity>,
    onMicTrigger: () -> Unit,
    onToggleContinuousRadar: (Boolean) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleVoiceFeedback: () -> Unit,
    onExecutePhrase: (String) -> Unit,
    onExecuteActionCode: (String, String) -> Unit,
    onClearLogs: () -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var typedCommand by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("commander_deck_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live System Telemetry Ribbon
        item {
            LiveSystemTelemetryRibbon(
                isAccessibilityConnected = isAccessibilityConnected,
                batteryPercent = uiState.batteryPercent,
                isCharging = uiState.isCharging,
                volumePercent = uiState.mediaVolumePercent,
                isTorchOn = uiState.isTorchActive,
                continuousRadarMode = uiState.continuousRadarMode,
                onAccessibilityClick = onNavigateToSetup
            )
        }

        // 2. Sonic Hero Card with Generated Soundwave Art + Visualizer Orb
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_voice_hero_1791777741100),
                        contentDescription = "Sonic acoustic background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        alpha = 0.25f
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DeepObsidian.copy(alpha = 0.55f),
                                        CardSurface.copy(alpha = 0.92f),
                                        CardSurface
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Controls Row: Language Selector + TTS Feedback + Continuous Radar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = true,
                                    onClick = onToggleLanguage,
                                    label = {
                                        Text(
                                            text = if (uiState.selectedLanguageTag == "hi-IN") "हिंदी / Hinglish" else "English (IN)",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = "Switch Language",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElevatedNavy,
                                        selectedLabelColor = ElectricCyan,
                                        selectedLeadingIconColor = ElectricCyan
                                    ),
                                    modifier = Modifier.testTag("language_toggle_chip")
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = onToggleVoiceFeedback,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(ElevatedNavy)
                                            .testTag("tts_feedback_toggle")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.RecordVoiceOver,
                                            contentDescription = "Toggle Spoken TTS Response",
                                            tint = if (uiState.voiceFeedbackEnabled) NeonEmerald else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(ElevatedNavy)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Radar,
                                            contentDescription = "Continuous Radar",
                                            tint = if (uiState.continuousRadarMode) NeonEmerald else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Auto Mic",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = uiState.continuousRadarMode,
                                            onCheckedChange = onToggleContinuousRadar,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = DeepObsidian,
                                                checkedTrackColor = NeonEmerald
                                            ),
                                            modifier = Modifier.testTag("continuous_radar_switch")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Central Sonic Voice Visualizer Orb
                            SonicVoiceVisualizerOrb(
                                isListening = uiState.isListening,
                                continuousRadarMode = uiState.continuousRadarMode,
                                rmsLevel = uiState.soundLevelRms,
                                onMicClick = onMicTrigger
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Live Voice Transcript Pill
                            Text(
                                text = when {
                                    uiState.isListening -> uiState.liveTranscript.ifBlank { "Sun raha hoon… Boliye!" }
                                    uiState.liveTranscript.isNotBlank() -> "Heard: ${uiState.liveTranscript}"
                                    else -> "Mic dabayein ya 'Voice Pad' kholein: \"Open YouTube\""
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (uiState.isListening) NeonEmerald else ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Live Microphone Signal Level & Diagnostic Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DeepObsidian.copy(alpha = 0.65f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.diagnosticStatusText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (uiState.isHardwareAudioDetected) NeonEmerald else WarmAmber,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Mic Audio Signal: ${uiState.micSignalDb}% (Browser Emulator Mic = 0% → Tap Voice Pad)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onExecutePhrase("Open YouTube") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralRed,
                                        contentColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("hero_open_youtube_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Open YouTube",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "YouTube",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Last Executed Action Feedback Banner
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (uiState.lastResultSuccess) NeonEmerald.copy(alpha = 0.4f) else WarmAmber.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                color = ElevatedNavy.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Command status",
                                        tint = if (uiState.lastResultSuccess) NeonEmerald else WarmAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.lastResultTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = uiState.lastResultSpokenFeedback,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Accessibility Prompt Banner if user tried a global gesture without Accessibility enabled
        item {
            AnimatedVisibility(visible = !isAccessibilityConnected || uiState.showAccessibilityPromptBanner) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmAmber.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                    color = Color(0xFF281F0C),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Full Phone Control (Back, Home, Scroll, Lock)",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Doosre apps ke andar Scroll, Back, Home aur Screenshot aawaz se chalane ke liye Accessibility Service ON karein.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } catch (_: Exception) {
                                    onNavigateToSetup()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmAmber,
                                contentColor = DeepObsidian
                            ),
                            modifier = Modifier.testTag("enable_accessibility_banner_btn")
                        ) {
                            Text("Enable", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Instant Voice Command Simulation Chips + Text Input Console
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instant Voice Command Console",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap any Hinglish phrase below or type a custom voice command to test & execute immediately:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("quick_voice_chips_row")
                    ) {
                        items(QUICK_VOICE_CHIPS) { phrase ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onExecutePhrase(phrase) }
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(50))
                                    .testTag("voice_chip_${phrase.lowercase(Locale.ROOT).replace(" ", "_")}"),
                                color = ElevatedNavy,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "\"$phrase\"",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = typedCommand,
                            onValueChange = { typedCommand = it },
                            placeholder = {
                                Text(
                                    text = "Boliye ya likhein: e.g. \"Torch chalu karo\", \"Call 9876543210\"…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (typedCommand.isNotBlank()) {
                                        onExecutePhrase(typedCommand)
                                        typedCommand = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = ElevatedNavy,
                                unfocusedContainerColor = ElevatedNavy
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_command_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (typedCommand.isNotBlank()) {
                                    onExecutePhrase(typedCommand)
                                    typedCommand = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = DeepObsidian
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
                            modifier = Modifier.testTag("execute_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Execute Command"
                            )
                        }
                    }
                }
            }
        }

        // 5. Hands-Free System Action Deck (16 One-Tap / Voice-Linked Controls)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Phone Control Matrix (Awaaz + Touch Deck)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Har card par uska Hindi/Hinglish voice command likha hai — bolkar ya tap karke turant chalayein:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2
                ) {
                    SYSTEM_DECK_BUTTONS.forEach { btn ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onExecuteActionCode(btn.actionCode, btn.voicePhrase) }
                                .border(1.dp, btn.accent.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                                .testTag("deck_btn_${btn.actionCode.lowercase(Locale.ROOT)}"),
                            color = CardSurface,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(btn.accent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = btn.icon,
                                        contentDescription = btn.labelEn,
                                        tint = btn.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = btn.labelHi,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "\"${btn.voicePhrase}\"",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = btn.accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Voice Command Execution Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Command Execution Log",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear Logs",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = TextSecondary)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                    color = CardSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Abhi tak koi voice command log nahi hai.",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Upar kisi bhi command chip par tap karein ya Mic bolkar test karein!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(logs.take(10), key = { it.id }) { log ->
                CommandLogRow(log = log, onReplay = { onExecutePhrase(log.spokenPhrase) })
            }
        }
    }
}

@Composable
private fun CommandLogRow(
    log: CommandLogEntity,
    onReplay: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onReplay)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        color = CardSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (log.isSuccess) NeonEmerald else WarmAmber)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "\"${log.spokenPhrase}\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = timeFormatter.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
                Text(
                    text = "${log.matchedActionTitle} • ${log.responseMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
