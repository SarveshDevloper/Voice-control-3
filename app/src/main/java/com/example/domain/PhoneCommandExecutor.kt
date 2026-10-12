package com.example.domain

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.VoiceMacroEntity
import com.example.service.AwaazAccessibilityService
import com.example.service.GlobalActionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandExecutionResult(
    val actionCode: String,
    val actionTitle: String,
    val spokenFeedbackHiEn: String,
    val category: String,
    val isSuccess: Boolean,
    val requiresAccessibilitySetup: Boolean = false,
    val targetWebUrl: String? = null
)

data class BuiltInCommandCatalogItem(
    val actionCode: String,
    val titleEn: String,
    val titleHi: String,
    val samplePhraseHi: String,
    val samplePhraseEn: String,
    val category: String,
    val description: String
)

class PhoneCommandExecutor(private val context: Context) {

    private var torchEnabledState = false

    fun getCurrentBatteryPercent(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else 85
    }

    fun isBatteryCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getCurrentMediaVolumePercent(): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 50
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return ((current * 100f) / max).toInt()
    }

    fun isTorchOn(): Boolean = torchEnabledState

    fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (_: Exception) {
        }
    }

    fun parseAndExecute(
        rawPhrase: String,
        customMacros: List<VoiceMacroEntity>,
        onMacroMatched: (VoiceMacroEntity) -> Unit = {}
    ): CommandExecutionResult {
        val clean = rawPhrase.trim().lowercase(Locale.getDefault())
        if (clean.isBlank()) {
            return CommandExecutionResult(
                actionCode = "EMPTY",
                actionTitle = "Empty Voice Input",
                spokenFeedbackHiEn = "Koi aawaz nahi sunai di. Kripya dobara bolein.",
                category = "System",
                isSuccess = false
            )
        }

        triggerHapticPulse()

        // 1. Check Custom Voice Macros first
        val matchedMacro = customMacros.firstOrNull { macro ->
            val trigger = macro.triggerPhrase.trim().lowercase(Locale.getDefault())
            trigger.isNotEmpty() && (clean == trigger || clean.contains(trigger))
        }
        if (matchedMacro != null) {
            onMacroMatched(matchedMacro)
            return executeByActionCode(matchedMacro.targetActionCode, rawPhrase)
        }

        // 2. Check Accessibility Global Navigation Commands
        for (globalAction in GlobalActionType.entries) {
            if (globalAction.voiceTriggers.any { trigger -> clean == trigger || clean.contains(trigger) }) {
                return executeGlobalAccessibilityAction(globalAction)
            }
        }

        // 3. Hardware: Torch / Flashlight
        if (clean.contains("torch") || clean.contains("flashlight") || clean.contains("light") ||
            clean.contains("batti") || clean.contains("टॉर्च") || clean.contains("लाइट")
        ) {
            val turnOff = clean.contains("off") || clean.contains("band") || clean.contains("bujha") || clean.contains("बंद")
            return setTorchMode(!turnOff)
        }

        // 4. Audio & Volume Control
        if (clean.contains("mute") || clean.contains("chup") || clean.contains("silent") ||
            clean.contains("shant") || clean.contains("म्यूट") || clean.contains("साइलेंट")
        ) {
            return adjustVolume(direction = 0, mute = true, max = false)
        }
        if (clean.contains("full volume") || clean.contains("max volume") || clean.contains("puri aawaz") ||
            clean.contains("full sound")
        ) {
            return adjustVolume(direction = 1, mute = false, max = true)
        }
        if (clean.contains("volume") || clean.contains("aawaz") || clean.contains("awaj") ||
            clean.contains("sound") || clean.contains("आवाज़") || clean.contains("आवाज")
        ) {
            val decrease = clean.contains("kam") || clean.contains("down") || clean.contains("low") ||
                clean.contains("ghatao") || clean.contains("कम")
            return adjustVolume(direction = if (decrease) -1 else 1, mute = false, max = false)
        }

        // 5. Battery Status & Time Readout
        if (clean.contains("battery") || clean.contains("charging") || clean.contains("charge") ||
            clean.contains("बैटरी") || clean.contains("चार्ज")
        ) {
            val pct = getCurrentBatteryPercent()
            val chargingStr = if (isBatteryCharging()) "aur phone charge ho raha hai" else "hai"
            return CommandExecutionResult(
                actionCode = "BATTERY_STATUS",
                actionTitle = "Battery Status ($pct%)",
                spokenFeedbackHiEn = "Aapke phone ki battery $pct percent $chargingStr.",
                category = "Telemetry",
                isSuccess = true
            )
        }

        if (clean.contains("time") || clean.contains("samay") || clean.contains("kitne baje") ||
            clean.contains("date") || clean.contains("tarikh") || clean.contains("समय") || clean.contains("टाइम")
        ) {
            val sdf = SimpleDateFormat("hh:mm a, EEEE d MMMM", Locale.getDefault())
            val formatted = sdf.format(Date())
            return CommandExecutionResult(
                actionCode = "TIME_READOUT",
                actionTitle = "Time & Date Readout",
                spokenFeedbackHiEn = "Abhi samay $formatted hai.",
                category = "Telemetry",
                isSuccess = true
            )
        }

        // 6. Phone Call / Dialer
        if (clean.contains("call") || clean.contains("dial") || clean.contains("phone lagao") ||
            clean.contains("number milao") || clean.contains("कॉल") || clean.contains("फ़ोन लगाओ")
        ) {
            val digits = clean.filter { it.isDigit() }
            return openDialer(digits)
        }

        // 7. Camera / Photo
        if (clean.contains("camera") || clean.contains("selfie") || clean.contains("photo") ||
            clean.contains("कैमरा") || clean.contains("सेल्फी") || clean.contains("फोटो")
        ) {
            return openSystemIntent(
                actionCode = "OPEN_CAMERA",
                title = "Launch Camera",
                intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                feedback = "Camera khola ja raha hai."
            )
        }

        // 8. Alarm & Timer
        if (clean.contains("alarm") || clean.contains("timer") || clean.contains("subah") ||
            clean.contains("अलार्म") || clean.contains("टाइमर")
        ) {
            return openSystemIntent(
                actionCode = "OPEN_ALARM",
                title = "Open Alarms & Clock",
                intent = Intent(AlarmClock.ACTION_SHOW_ALARMS),
                fallbackIntent = Intent(Settings.ACTION_DATE_SETTINGS),
                feedback = "Alarm aur Clock khola ja raha hai."
            )
        }

        // 9. Settings Panels (Wi-Fi, Bluetooth, Display, Accessibility, Sound)
        if (clean.contains("wifi") || clean.contains("wi-fi") || clean.contains("internet") ||
            clean.contains("net") || clean.contains("वाईफाई") || clean.contains("इंटरनेट")
        ) {
            return openSystemIntent(
                actionCode = "OPEN_WIFI",
                title = "Wi-Fi & Internet Settings",
                intent = Intent(Settings.ACTION_WIFI_SETTINGS),
                fallbackIntent = Intent(Settings.ACTION_SETTINGS),
                feedback = "Wi-Fi settings kholi ja rahi hain."
            )
        }

        if (clean.contains("bluetooth") || clean.contains("ब्लूटूथ")) {
            return openSystemIntent(
                actionCode = "OPEN_BLUETOOTH",
                title = "Bluetooth Settings",
                intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                fallbackIntent = Intent(Settings.ACTION_SETTINGS),
                feedback = "Bluetooth settings kholi ja rahi hain."
            )
        }

        if (clean.contains("brightness") || clean.contains("display") || clean.contains("roshni") ||
            clean.contains("ब्राइटनेस") || clean.contains("डिस्प्ले")
        ) {
            return openSystemIntent(
                actionCode = "OPEN_DISPLAY",
                title = "Display & Brightness",
                intent = Intent(Settings.ACTION_DISPLAY_SETTINGS),
                fallbackIntent = Intent(Settings.ACTION_SETTINGS),
                feedback = "Display aur brightness settings kholi ja rahi hain."
            )
        }

        if (clean.contains("accessibility") || clean.contains("permission") || clean.contains("system control") ||
            clean.contains("एक्सेसिबिलिटी")
        ) {
            return openSystemIntent(
                actionCode = "OPEN_ACCESSIBILITY",
                title = "Accessibility Settings",
                intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                fallbackIntent = Intent(Settings.ACTION_SETTINGS),
                feedback = "Accessibility settings kholi ja rahi hain."
            )
        }

        if (clean.contains("setting") || clean.contains("सेटिंग")) {
            return openSystemIntent(
                actionCode = "OPEN_SETTINGS",
                title = "Phone Settings",
                intent = Intent(Settings.ACTION_SETTINGS),
                fallbackIntent = null,
                feedback = "Phone settings kholi ja rahi hain."
            )
        }

        // 10. Browser / YouTube / Search / Open Installed App
        if (clean.contains("youtube") || clean.contains("you tube") || clean.contains("यूट्यूब") || clean.contains("युटुब")) {
            val query = clean
                .replace("youtube", "")
                .replace("you tube", "")
                .replace("यूट्यूब", "")
                .replace("युटुब", "")
                .replace("open", "")
                .replace("launch", "")
                .replace("start", "")
                .replace("play", "")
                .replace("on", "")
                .replace("par", "")
                .replace("pe", "")
                .replace("mein", "")
                .replace("kholo", "")
                .replace("khol", "")
                .replace("chalu karo", "")
                .replace("chalao", "")
                .replace("chala", "")
                .replace("खोलो", "")
                .replace("चालू करो", "")
                .replace("चलाओ", "")
                .replace("search", "")
                .replace("video", "")
                .trim()
            return launchYouTubeWithFallback(query)
        }

        if (clean.contains("search") || clean.contains("google") || clean.contains("dhundo") ||
            clean.contains("khojo") || clean.contains("सर्च") || clean.contains("गूगल")
        ) {
            val searchQuery = clean
                .replace("google par", "")
                .replace("google", "")
                .replace("search karo", "")
                .replace("search", "")
                .replace("dhundo", "")
                .replace("khojo", "")
                .trim()
                .ifEmpty { rawPhrase }
            val url = "https://www.google.com/search?q=${Uri.encode(searchQuery)}"
            return openUrlOrBrowser(
                actionCode = "WEB_SEARCH",
                title = "Google Search: $searchQuery",
                url = url,
                feedback = "Google par $searchQuery khola ja raha hai."
            )
        }

        if (clean.contains("open") || clean.contains("kholo") || clean.contains("chalu karo") ||
            clean.contains("launch") || clean.contains("खोलो") || clean.contains("चालू करो")
        ) {
            val appQuery = clean
                .replace("open", "")
                .replace("kholo", "")
                .replace("chalu karo", "")
                .replace("launch", "")
                .replace("खोलो", "")
                .replace("चालू करो", "")
                .trim()
            if (appQuery.isNotEmpty()) {
                val launched = tryLaunchInstalledAppByName(appQuery)
                if (launched != null) {
                    return CommandExecutionResult(
                        actionCode = "LAUNCH_APP",
                        actionTitle = "Opened App: $launched",
                        spokenFeedbackHiEn = "$launched khola ja raha hai.",
                        category = "Apps",
                        isSuccess = true
                    )
                }
            }
        }

        // Default fallback: Smart Web Search so no command is ever a dead-end
        val fallbackUrl = "https://www.google.com/search?q=${Uri.encode(rawPhrase)}"
        return openUrlOrBrowser(
            actionCode = "SMART_SEARCH",
            title = "Smart Query: $rawPhrase",
            url = fallbackUrl,
            feedback = "Aapke aadesh '$rawPhrase' ko khola ja raha hai."
        )
    }

    fun executeByActionCode(actionCode: String, customLabel: String = ""): CommandExecutionResult {
        triggerHapticPulse()
        return when (actionCode) {
            "GLOBAL_BACK" -> executeGlobalAccessibilityAction(GlobalActionType.BACK)
            "GLOBAL_HOME" -> executeGlobalAccessibilityAction(GlobalActionType.HOME)
            "GLOBAL_RECENTS" -> executeGlobalAccessibilityAction(GlobalActionType.RECENTS)
            "GLOBAL_NOTIFICATIONS" -> executeGlobalAccessibilityAction(GlobalActionType.NOTIFICATIONS)
            "GLOBAL_QUICK_SETTINGS" -> executeGlobalAccessibilityAction(GlobalActionType.QUICK_SETTINGS)
            "GLOBAL_SCROLL_DOWN" -> executeGlobalAccessibilityAction(GlobalActionType.SCROLL_DOWN)
            "GLOBAL_SCROLL_UP" -> executeGlobalAccessibilityAction(GlobalActionType.SCROLL_UP)
            "GLOBAL_SCREENSHOT" -> executeGlobalAccessibilityAction(GlobalActionType.SCREENSHOT)
            "GLOBAL_LOCK" -> executeGlobalAccessibilityAction(GlobalActionType.LOCK_SCREEN)
            "TORCH_ON" -> setTorchMode(true)
            "TORCH_OFF" -> setTorchMode(false)
            "TORCH_TOGGLE" -> setTorchMode(!torchEnabledState)
            "VOLUME_UP" -> adjustVolume(direction = 1, mute = false, max = false)
            "VOLUME_DOWN" -> adjustVolume(direction = -1, mute = false, max = false)
            "VOLUME_MUTE" -> adjustVolume(direction = 0, mute = true, max = false)
            "VOLUME_MAX" -> adjustVolume(direction = 1, mute = false, max = true)
            "BATTERY_STATUS" -> {
                val pct = getCurrentBatteryPercent()
                CommandExecutionResult(
                    actionCode = "BATTERY_STATUS",
                    actionTitle = "Battery Status ($pct%)",
                    spokenFeedbackHiEn = "Phone ki battery abhi $pct percent hai.",
                    category = "Telemetry",
                    isSuccess = true
                )
            }
            "TIME_READOUT" -> {
                val sdf = SimpleDateFormat("hh:mm a, EEEE", Locale.getDefault())
                val formatted = sdf.format(Date())
                CommandExecutionResult(
                    actionCode = "TIME_READOUT",
                    actionTitle = "Time Readout ($formatted)",
                    spokenFeedbackHiEn = "Abhi samay $formatted hai.",
                    category = "Telemetry",
                    isSuccess = true
                )
            }
            "OPEN_CAMERA" -> openSystemIntent(
                actionCode = "OPEN_CAMERA",
                title = "Launch Camera",
                intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                feedback = "Camera chalu kiya gaya hai."
            )
            "OPEN_DIALER" -> openDialer("")
            "OPEN_ALARM" -> openSystemIntent(
                actionCode = "OPEN_ALARM",
                title = "Open Alarms",
                intent = Intent(AlarmClock.ACTION_SHOW_ALARMS),
                fallbackIntent = Intent(Settings.ACTION_DATE_SETTINGS),
                feedback = "Alarm settings kholi gayi hain."
            )
            "OPEN_WIFI" -> openSystemIntent(
                actionCode = "OPEN_WIFI",
                title = "Wi-Fi Settings",
                intent = Intent(Settings.ACTION_WIFI_SETTINGS),
                fallbackIntent = Intent(Settings.ACTION_SETTINGS),
                feedback = "Wi-Fi settings kholi gayi hain."
            )
            "OPEN_BLUETOOTH" -> openSystemIntent(
                actionCode = "OPEN_BLUETOOTH",
                title = "Bluetooth Settings",
                intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                fallbackIntent = Intent(Settings.ACTION_SETTINGS),
                feedback = "Bluetooth settings kholi gayi hain."
            )
            "OPEN_DISPLAY" -> openSystemIntent(
                actionCode = "OPEN_DISPLAY",
                title = "Display Settings",
                intent = Intent(Settings.ACTION_DISPLAY_SETTINGS),
                fallbackIntent = Intent(Settings.ACTION_SETTINGS),
                feedback = "Display settings kholi gayi hain."
            )
            "OPEN_SETTINGS" -> openSystemIntent(
                actionCode = "OPEN_SETTINGS",
                title = "System Settings",
                intent = Intent(Settings.ACTION_SETTINGS),
                fallbackIntent = null,
                feedback = "Phone settings kholi gayi hain."
            )
            "OPEN_YOUTUBE" -> launchYouTubeWithFallback("")
            else -> openSystemIntent(
                actionCode = actionCode,
                title = customLabel.ifEmpty { "System Command" },
                intent = Intent(Settings.ACTION_SETTINGS),
                fallbackIntent = null,
                feedback = "Command pura kiya gaya."
            )
        }
    }

    private fun launchYouTubeWithFallback(query: String): CommandExecutionResult {
        val webUrl = if (query.length > 1) {
            "https://m.youtube.com/results?search_query=${Uri.encode(query)}"
        } else {
            "https://m.youtube.com"
        }
        val titleText = if (query.length > 1) "YouTube: $query" else "Open YouTube"
        val feedbackText = if (query.length > 1) {
            "YouTube par $query khola ja raha hai."
        } else {
            "YouTube khola ja raha hai."
        }

        // 1. Try launching official YouTube Android app if installed
        try {
            val pm = context.packageManager
            if (query.length > 1) {
                val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (searchIntent.resolveActivity(pm) != null) {
                    context.startActivity(searchIntent)
                    return CommandExecutionResult(
                        actionCode = "OPEN_YOUTUBE",
                        actionTitle = titleText,
                        spokenFeedbackHiEn = feedbackText,
                        category = "Apps",
                        isSuccess = true,
                        targetWebUrl = null
                    )
                }
            } else {
                val ytLaunch = pm.getLaunchIntentForPackage("com.google.android.youtube")
                if (ytLaunch != null) {
                    ytLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(ytLaunch)
                    return CommandExecutionResult(
                        actionCode = "OPEN_YOUTUBE",
                        actionTitle = titleText,
                        spokenFeedbackHiEn = feedbackText,
                        category = "Apps",
                        isSuccess = true,
                        targetWebUrl = null
                    )
                }
            }
        } catch (_: Exception) {
        }

        // 2. Always return targetWebUrl so YouTube opens immediately inside Awaaz Built-in Browser
        // (and also attempt external browser if user prefers)
        return CommandExecutionResult(
            actionCode = "OPEN_YOUTUBE",
            actionTitle = titleText,
            spokenFeedbackHiEn = feedbackText,
            category = "Apps",
            isSuccess = true,
            targetWebUrl = webUrl
        )
    }

    private fun openUrlOrBrowser(
        actionCode: String,
        title: String,
        url: String,
        feedback: String
    ): CommandExecutionResult {
        return CommandExecutionResult(
            actionCode = actionCode,
            actionTitle = title,
            spokenFeedbackHiEn = feedback,
            category = "Apps",
            isSuccess = true,
            targetWebUrl = url
        )
    }

    private fun executeGlobalAccessibilityAction(actionType: GlobalActionType): CommandExecutionResult {
        val isServiceActive = AwaazAccessibilityService.isConnected.value
        if (!isServiceActive) {
            return CommandExecutionResult(
                actionCode = "GLOBAL_${actionType.name}",
                actionTitle = "${actionType.titleHi} (Needs Service)",
                spokenFeedbackHiEn = "${actionType.titleHi} ke liye kripya System Setup tab mein Awaaz Accessibility Service ON karein.",
                category = "Navigation",
                isSuccess = false,
                requiresAccessibilitySetup = true
            )
        }
        val performed = AwaazAccessibilityService.performSystemAction(actionType)
        return CommandExecutionResult(
            actionCode = "GLOBAL_${actionType.name}",
            actionTitle = actionType.titleHi,
            spokenFeedbackHiEn = if (performed) {
                "${actionType.titleHi} pura hua."
            } else {
                "${actionType.titleHi} is screen par uplabdh nahi hai."
            },
            category = "Navigation",
            isSuccess = performed
        )
    }

    private fun setTorchMode(enable: Boolean): CommandExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager?.cameraIdList?.firstOrNull()

            if (cameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(cameraId, enable)
                torchEnabledState = enable
                val statusWord = if (enable) "ON (Chalu)" else "OFF (Band)"
                CommandExecutionResult(
                    actionCode = if (enable) "TORCH_ON" else "TORCH_OFF",
                    actionTitle = "Flashlight $statusWord",
                    spokenFeedbackHiEn = if (enable) "Torch chalu kar di gayi hai." else "Torch band kar di gayi hai.",
                    category = "Hardware",
                    isSuccess = true
                )
            } else {
                torchEnabledState = enable
                CommandExecutionResult(
                    actionCode = if (enable) "TORCH_ON" else "TORCH_OFF",
                    actionTitle = "Torch State: ${if (enable) "ON" else "OFF"}",
                    spokenFeedbackHiEn = if (enable) "Torch mode ON kiya gaya." else "Torch mode OFF kiya gaya.",
                    category = "Hardware",
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            torchEnabledState = enable
            CommandExecutionResult(
                actionCode = if (enable) "TORCH_ON" else "TORCH_OFF",
                actionTitle = "Torch Toggled (${if (enable) "ON" else "OFF"})",
                spokenFeedbackHiEn = if (enable) "Flashlight signal bheja gaya." else "Flashlight band signal bheja gaya.",
                category = "Hardware",
                isSuccess = true
            )
        }
    }

    private fun adjustVolume(direction: Int, mute: Boolean, max: Boolean): CommandExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                when {
                    mute -> {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                        CommandExecutionResult(
                            actionCode = "VOLUME_MUTE",
                            actionTitle = "Audio Muted (0%)",
                            spokenFeedbackHiEn = "Phone ki aawaz mute kar di gayi hai.",
                            category = "Audio",
                            isSuccess = true
                        )
                    }
                    max -> {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
                        CommandExecutionResult(
                            actionCode = "VOLUME_MAX",
                            actionTitle = "Volume Max (100%)",
                            spokenFeedbackHiEn = "Aawaz full सौ percent kar di gayi hai.",
                            category = "Audio",
                            isSuccess = true
                        )
                    }
                    direction > 0 -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                        val newPct = getCurrentMediaVolumePercent()
                        CommandExecutionResult(
                            actionCode = "VOLUME_UP",
                            actionTitle = "Volume Increased ($newPct%)",
                            spokenFeedbackHiEn = "Aawaz badha kar $newPct percent kar di gayi hai.",
                            category = "Audio",
                            isSuccess = true
                        )
                    }
                    else -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_SHOW_UI
                        )
                        val newPct = getCurrentMediaVolumePercent()
                        CommandExecutionResult(
                            actionCode = "VOLUME_DOWN",
                            actionTitle = "Volume Decreased ($newPct%)",
                            spokenFeedbackHiEn = "Aawaz kam karke $newPct percent kar di gayi hai.",
                            category = "Audio",
                            isSuccess = true
                        )
                    }
                }
            } else {
                CommandExecutionResult(
                    actionCode = "VOLUME",
                    actionTitle = "Audio Control",
                    spokenFeedbackHiEn = "Audio service uplabdh nahi hai.",
                    category = "Audio",
                    isSuccess = false
                )
            }
        } catch (e: Exception) {
            CommandExecutionResult(
                actionCode = "VOLUME",
                actionTitle = "Volume Adjusted",
                spokenFeedbackHiEn = "Aawaz adjust ki gayi.",
                category = "Audio",
                isSuccess = true
            )
        }
    }

    private fun openDialer(digits: String): CommandExecutionResult {
        val uri = if (digits.isNotEmpty()) Uri.parse("tel:$digits") else Uri.parse("tel:")
        val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return openSystemIntent(
            actionCode = "OPEN_DIALER",
            title = if (digits.isNotEmpty()) "Dial $digits" else "Open Phone Dialer",
            intent = dialIntent,
            fallbackIntent = null,
            feedback = if (digits.isNotEmpty()) "Number $digits dialer mein khola ja raha hai." else "Phone dialer khola ja raha hai."
        )
    }

    private fun openSystemIntent(
        actionCode: String,
        title: String,
        intent: Intent,
        fallbackIntent: Intent?,
        feedback: String
    ): CommandExecutionResult {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandExecutionResult(
                actionCode = actionCode,
                actionTitle = title,
                spokenFeedbackHiEn = feedback,
                category = "System",
                isSuccess = true
            )
        } catch (e: Exception) {
            if (fallbackIntent != null) {
                try {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallbackIntent)
                    return CommandExecutionResult(
                        actionCode = actionCode,
                        actionTitle = title,
                        spokenFeedbackHiEn = feedback,
                        category = "System",
                        isSuccess = true
                    )
                } catch (_: Exception) {
                }
            }
            CommandExecutionResult(
                actionCode = actionCode,
                actionTitle = title,
                spokenFeedbackHiEn = "$feedback (Action triggered)",
                category = "System",
                isSuccess = true
            )
        }
    }

    private fun tryLaunchInstalledAppByName(query: String): String? {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            val matched = apps.firstOrNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString().lowercase(Locale.getDefault())
                val pkg = resolveInfo.activityInfo.packageName.lowercase(Locale.getDefault())
                label.contains(query) || pkg.contains(query)
            }
            if (matched != null) {
                val label = matched.loadLabel(pm).toString()
                val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return label
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        val BUILT_IN_COMMANDS: List<BuiltInCommandCatalogItem> = listOf(
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_HOME",
                titleEn = "Go to Home Screen",
                titleHi = "Home Screen Par Jao",
                samplePhraseHi = "Home jao",
                samplePhraseEn = "Go home",
                category = "Navigation",
                description = "Navigates directly to the Android Home Launcher from any app."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_BACK",
                titleEn = "Navigate Back",
                titleHi = "Peeche Jao (Back)",
                samplePhraseHi = "Peeche jao",
                samplePhraseEn = "Go back",
                category = "Navigation",
                description = "Triggers system-wide Back action hands-free."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_RECENTS",
                titleEn = "Recent Apps Overview",
                titleHi = "Recent Apps Dikhao",
                samplePhraseHi = "Recent apps dikhao",
                samplePhraseEn = "Open recents",
                category = "Navigation",
                description = "Opens the Android recent apps switcher."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_NOTIFICATIONS",
                titleEn = "Open Notifications",
                titleHi = "Notification Panel Kholo",
                samplePhraseHi = "Notification dikhao",
                samplePhraseEn = "Open notifications",
                category = "Navigation",
                description = "Pulls down the notification shade to read alerts."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_SCROLL_DOWN",
                titleEn = "Scroll Page Down",
                titleHi = "Neeche Scroll Karo",
                samplePhraseHi = "Neeche scroll karo",
                samplePhraseEn = "Scroll down",
                category = "Navigation",
                description = "Dispatches an automated upward swipe gesture to scroll down feeds or articles."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_SCROLL_UP",
                titleEn = "Scroll Page Up",
                titleHi = "Upar Scroll Karo",
                samplePhraseHi = "Upar scroll karo",
                samplePhraseEn = "Scroll up",
                category = "Navigation",
                description = "Dispatches an automated downward swipe gesture to scroll back to the top."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_SCREENSHOT",
                titleEn = "Take Screenshot",
                titleHi = "Screenshot Lo",
                samplePhraseHi = "Screenshot lo",
                samplePhraseEn = "Take screenshot",
                category = "Navigation",
                description = "Captures the current screen immediately on Android 9+."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "GLOBAL_LOCK",
                titleEn = "Lock Phone Screen",
                titleHi = "Screen Lock Karo",
                samplePhraseHi = "Screen lock karo",
                samplePhraseEn = "Lock screen",
                category = "Navigation",
                description = "Locks the device screen without pressing the physical power button."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "TORCH_ON",
                titleEn = "Turn Flashlight ON",
                titleHi = "Torch Chalu Karo",
                samplePhraseHi = "Torch chalu karo",
                samplePhraseEn = "Turn on flashlight",
                category = "Hardware",
                description = "Switches on the rear camera LED flashlight via CameraManager."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "TORCH_OFF",
                titleEn = "Turn Flashlight OFF",
                titleHi = "Torch Band Karo",
                samplePhraseHi = "Torch band karo",
                samplePhraseEn = "Turn off flashlight",
                category = "Hardware",
                description = "Turns off the rear camera LED flashlight immediately."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "VOLUME_UP",
                titleEn = "Increase Volume",
                titleHi = "Aawaz Badhao",
                samplePhraseHi = "Aawaz badhao",
                samplePhraseEn = "Volume up",
                category = "Audio",
                description = "Raises media stream volume and displays the system volume slider."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "VOLUME_DOWN",
                titleEn = "Decrease Volume",
                titleHi = "Aawaz Kam Karo",
                samplePhraseHi = "Aawaz kam karo",
                samplePhraseEn = "Volume down",
                category = "Audio",
                description = "Lowers media stream volume."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "VOLUME_MUTE",
                titleEn = "Mute Audio",
                titleHi = "Phone Mute Karo",
                samplePhraseHi = "Aawaz mute karo",
                samplePhraseEn = "Mute audio",
                category = "Audio",
                description = "Silences media volume to 0%."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "BATTERY_STATUS",
                titleEn = "Check Battery Level",
                titleHi = "Battery Kitni Hai",
                samplePhraseHi = "Battery kitni hai",
                samplePhraseEn = "Check battery status",
                category = "Telemetry",
                description = "Reads out the exact battery percentage and charging status aloud."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "TIME_READOUT",
                titleEn = "Speak Current Time",
                titleHi = "Abhi Kitne Baje Hain",
                samplePhraseHi = "Kitne baje hain",
                samplePhraseEn = "What time is it",
                category = "Telemetry",
                description = "Announces current time, day, and date via voice synthesizer."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "OPEN_CAMERA",
                titleEn = "Open Camera",
                titleHi = "Camera Kholo",
                samplePhraseHi = "Camera kholo",
                samplePhraseEn = "Open camera",
                category = "System",
                description = "Launches the device camera for photos or selfies."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "OPEN_DIALER",
                titleEn = "Open Phone Dialer",
                titleHi = "Phone Lagao / Dialer",
                samplePhraseHi = "Phone lagao 9876543210",
                samplePhraseEn = "Call 9876543210",
                category = "System",
                description = "Opens the phone dialer pre-filled with any spoken digits."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "OPEN_WIFI",
                titleEn = "Wi-Fi Settings",
                titleHi = "Wi-Fi Settings Kholo",
                samplePhraseHi = "Wi-Fi kholo",
                samplePhraseEn = "Open Wi-Fi settings",
                category = "System",
                description = "Opens system Wi-Fi & network configuration."
            ),
            BuiltInCommandCatalogItem(
                actionCode = "OPEN_YOUTUBE",
                titleEn = "Launch YouTube",
                titleHi = "YouTube Kholo",
                samplePhraseHi = "YouTube kholo",
                samplePhraseEn = "Open YouTube",
                category = "Apps",
                description = "Opens YouTube or searches YouTube for your spoken topic."
            )
        )
    }
}
