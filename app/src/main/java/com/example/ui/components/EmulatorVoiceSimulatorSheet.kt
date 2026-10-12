package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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

private data class QuickSpokenCommand(
    val phrase: String,
    val subtitleHi: String,
    val accent: Color
)

private val INSTANT_SPOKEN_COMMANDS = listOf(
    QuickSpokenCommand("Open YouTube", "YouTube Turant Kholein", CoralRed),
    QuickSpokenCommand("YouTube Arijit Singh songs", "YouTube Par Gane Chalayein", CoralRed),
    QuickSpokenCommand("Torch chalu karo", "Flashlight ON Karein", WarmAmber),
    QuickSpokenCommand("Torch band karo", "Flashlight OFF Karein", WarmAmber),
    QuickSpokenCommand("Battery kitni hai", "Battery % Bolkar Suniye", NeonEmerald),
    QuickSpokenCommand("Kitne baje hain", "Abhi Ka Time Suniye", NeonEmerald),
    QuickSpokenCommand("Aawaz badhao", "Volume + Badhayein", ElectricCyan),
    QuickSpokenCommand("Aawaz mute karo", "Phone Silent/Mute Karein", ElectricCyan),
    QuickSpokenCommand("Neeche scroll karo", "Page Neeche Scroll Karein", NeonEmerald),
    QuickSpokenCommand("Upar scroll karo", "Page Upar Scroll Karein", NeonEmerald),
    QuickSpokenCommand("Home jao", "Home Screen Par Jaayein", ElectricCyan),
    QuickSpokenCommand("Peeche jao", "Back Button Dabayein", ElectricCyan),
    QuickSpokenCommand("Camera kholo", "Camera App Chalu Karein", WarmAmber),
    QuickSpokenCommand("Wi-Fi kholo", "Wi-Fi Settings Kholein", ElectricCyan),
    QuickSpokenCommand("Google par Mausam search karo", "Google Weather Search", NeonEmerald)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmulatorVoiceSimulatorSheet(
    diagnosticStatusText: String,
    onExecutePhrase: (String) -> Unit,
    onRetryHardwareMic: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customSpokenText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface,
        scrimColor = DeepObsidian.copy(alpha = 0.78f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .testTag("emulator_voice_simulator_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Instant Voice Command Pad",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Emulator / Browser Mic Bridge",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonEmerald
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Explanation Banner why Emulator Mic didn't hear laptop audio
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WarmAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                color = Color(0xFF281F0C),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Aawaz kyun nahi sunai di? (Why Mic Was Silent)",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmAmber
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AI Studio ka Browser Preview Emulator cloud server par chalta hai, jahan aapke phone/laptop ka microphone audio forward nahi hota (0% audio signal). Real phone APK mein aapki aawaz 100% kaam karegi! Abhi yahan kisi bhi command par Tap karein ya type karein:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Voice Input Box (Works with Phone Keyboard's Voice Mic too!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customSpokenText,
                    onValueChange = { customSpokenText = it },
                    placeholder = {
                        Text(
                            text = "Yahan bolein (Keyboard Mic) ya likhein: \"Open YouTube\"…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (customSpokenText.isNotBlank()) {
                                onExecutePhrase(customSpokenText)
                                customSpokenText = ""
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
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (customSpokenText.isNotBlank()) {
                            onExecutePhrase(customSpokenText)
                            customSpokenText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonEmerald,
                        contentColor = DeepObsidian
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Voice Command"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tap Any Voice Command to Execute Immediately:",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                INSTANT_SPOKEN_COMMANDS.forEach { cmd ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onExecutePhrase(cmd.phrase) }
                            .border(1.dp, cmd.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                        color = ElevatedNavy,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = cmd.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "\"${cmd.phrase}\"",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = cmd.subtitleHi,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cmd.accent,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diagnosticStatusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRetryHardwareMic,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedNavy,
                        contentColor = ElectricCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Mic")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
