package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.VoiceMacroEntity
import com.example.domain.PhoneCommandExecutor
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
import java.util.Locale

private data class MacroTargetOption(
    val actionCode: String,
    val labelHiEn: String,
    val category: String
)

private val AVAILABLE_MACRO_TARGETS = listOf(
    MacroTargetOption("TORCH_ON", "Torch ON (Roshni Chalu)", "Hardware"),
    MacroTargetOption("TORCH_OFF", "Torch OFF (Roshni Band)", "Hardware"),
    MacroTargetOption("GLOBAL_HOME", "Go Home Screen", "Navigation"),
    MacroTargetOption("GLOBAL_BACK", "Navigate Back (Peeche)", "Navigation"),
    MacroTargetOption("GLOBAL_SCROLL_DOWN", "Scroll Down Page", "Navigation"),
    MacroTargetOption("GLOBAL_SCREENSHOT", "Take Screenshot", "Navigation"),
    MacroTargetOption("VOLUME_MUTE", "Mute Phone Audio", "Audio"),
    MacroTargetOption("VOLUME_MAX", "Full 100% Volume", "Audio"),
    MacroTargetOption("OPEN_CAMERA", "Launch Camera", "Media"),
    MacroTargetOption("OPEN_ALARM", "Open Alarms & Clock", "Productivity"),
    MacroTargetOption("OPEN_WIFI", "Open Wi-Fi Settings", "Settings"),
    MacroTargetOption("OPEN_YOUTUBE", "Open YouTube", "Apps"),
    MacroTargetOption("BATTERY_STATUS", "Read Battery Level", "Telemetry"),
    MacroTargetOption("TIME_READOUT", "Speak Current Time", "Telemetry")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomShortcutsScreen(
    macros: List<VoiceMacroEntity>,
    onAddMacro: (triggerPhrase: String, actionCode: String, description: String, category: String) -> Unit,
    onDeleteMacro: (Int) -> Unit,
    onRunMacro: (VoiceMacroEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var triggerInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf(AVAILABLE_MACRO_TARGETS.first()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("custom_shortcuts_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apna Voice Shortcut Banayein",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Koi bhi pasandida Hindi/English shabd chunein (jaise \"Andhera hai\" ya \"Office mode\") aur usse phone ke action se jodein.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = triggerInput,
                        onValueChange = { triggerInput = it },
                        label = { Text("Jab main bolun (Trigger Phrase)") },
                        placeholder = { Text("e.g., Chai time, Gud night, Roshni karo") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = ElectricCyan,
                            unfocusedLabelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_trigger_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tab phone yeh action karega:",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AVAILABLE_MACRO_TARGETS.forEach { option ->
                            val isSelected = selectedTarget.actionCode == option.actionCode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTarget = option },
                                label = { Text(option.labelHiEn) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan,
                                    containerColor = ElevatedNavy,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Description / Note (Optional)") },
                        placeholder = { Text("e.g., Raat ko sote waqt phone silent karne ke liye") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_description_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (triggerInput.isNotBlank()) {
                                onAddMacro(
                                    triggerInput,
                                    selectedTarget.actionCode,
                                    descriptionInput.ifBlank { selectedTarget.labelHiEn },
                                    selectedTarget.category
                                )
                                triggerInput = ""
                                descriptionInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = DeepObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_macro_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Voice Shortcut", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Saved Voice Shortcuts (${macros.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        items(macros, key = { it.id }) { macro ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onRunMacro(macro) }
                    .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                    .testTag("macro_card_${macro.id}"),
                color = CardSurface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Run shortcut",
                            tint = ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\"${macro.triggerPhrase}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ElevatedNavy,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = macro.category,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = macro.descriptionHi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Action: ${macro.targetActionCode} • Used ${macro.usageCount}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                    IconButton(
                        onClick = { onDeleteMacro(macro.id) },
                        modifier = Modifier.testTag("delete_macro_${macro.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Macro",
                            tint = CoralRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BilingualCommandGuideScreen(
    onTryPhrase: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Navigation", "Hardware", "Audio", "Telemetry", "System", "Apps")

    val filteredCommands = remember(searchQuery, selectedCategory) {
        PhoneCommandExecutor.BUILT_IN_COMMANDS.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                item.titleHi.contains(searchQuery, ignoreCase = true) ||
                item.titleEn.contains(searchQuery, ignoreCase = true) ||
                item.samplePhraseHi.contains(searchQuery, ignoreCase = true) ||
                item.samplePhraseEn.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("command_guide_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Hindi + English Voice Command Directory",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Aap Hindi, Hinglish ya English mein bolkar pura phone chala sakte hain. Kisi bhi card par tap karke abhi test karein:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = ElectricCyan)
                },
                placeholder = { Text("Search command (e.g., scroll, torch, home, battery)…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("guide_search_input")
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(4).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = DeepObsidian,
                            containerColor = CardSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        items(filteredCommands, key = { it.actionCode }) { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onTryPhrase(item.samplePhraseHi) }
                    .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                    .testTag("guide_item_${item.actionCode.lowercase(Locale.ROOT)}"),
                color = CardSurface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.titleHi,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${item.titleEn}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hindi: \"${item.samplePhraseHi}\"  |  English: \"${item.samplePhraseEn}\"",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = NeonEmerald.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Try command",
                                tint = NeonEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Try",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemSetupScreen(
    isAccessibilityConnected: Boolean,
    hasMicPermission: Boolean,
    voiceFeedbackEnabled: Boolean,
    onRequestMicPermission: () -> Unit,
    onToggleVoiceFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("system_setup_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Full Phone Voice Control Setup",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Apne phone ko 100% bina haath lagaye (Hands-Free) aawaz se chalane ke liye yeh 3 system steps check karein:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Step 1: Accessibility Service
        item {
            SetupStepCard(
                stepNumber = "STEP 01 • SYSTEM GLOBAL CONTROL",
                title = "Awaaz Accessibility Service",
                description = "Isse ON karne par aap kisi bhi app (WhatsApp, YouTube, Browser) ke andar bolkar Back, Home, Neeche Scroll, Upar Scroll, Notifications aur Screen Lock kar sakte hain.",
                isCompleted = isAccessibilityConnected,
                statusText = if (isAccessibilityConnected) "Connected & Active" else "Action Required: Enable in Settings",
                buttonText = if (isAccessibilityConnected) "Open Accessibility Settings" else "Enable Accessibility Service",
                icon = Icons.Filled.AccessibilityNew,
                onActionClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    } catch (_: Exception) {
                    }
                }
            )
        }

        // Step 2: Microphone Permission
        item {
            SetupStepCard(
                stepNumber = "STEP 02 • MICROPHONE ACCESS",
                title = "Voice Recognition Microphone",
                description = "Aapki aawaz sunne aur Hindi/English commands ko turant pehchanne ke liye Microphone permission zaroori hai.",
                isCompleted = hasMicPermission,
                statusText = if (hasMicPermission) "Microphone Ready" else "Grant Audio Permission",
                buttonText = if (hasMicPermission) "Microphone Granted" else "Allow Microphone Access",
                icon = Icons.Filled.Mic,
                onActionClick = onRequestMicPermission
            )
        }

        // Step 3: Spoken Voice Response (TTS)
        item {
            SetupStepCard(
                stepNumber = "STEP 03 • SPOKEN FEEDBACK (TTS)",
                title = "Boliye & Suniye (Voice Talkback)",
                description = "Jab aap phone ko command denge (jaise \"Battery kitni hai\"), phone aapko bolkar jawab dega.",
                isCompleted = voiceFeedbackEnabled,
                statusText = if (voiceFeedbackEnabled) "Voice Talkback Active" else "Voice Talkback Muted",
                buttonText = if (voiceFeedbackEnabled) "Mute Talkback" else "Enable Talkback",
                icon = Icons.Filled.RecordVoiceOver,
                onActionClick = onToggleVoiceFeedback
            )
        }

        // System Settings Quick Jump
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
                color = CardSurface,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SettingsSuggest,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android Voice Recognition Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Offline Hindi/English speech pack check karne ke liye phone settings kholein.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            } catch (_: Exception) {
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElevatedNavy,
                            contentColor = ElectricCyan
                        )
                    ) {
                        Text("Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    statusText: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onActionClick: () -> Unit
) {
    val accent = if (isCompleted) NeonEmerald else WarmAmber
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = DeepObsidian
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
