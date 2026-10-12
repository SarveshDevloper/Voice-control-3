package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GlobalActionType(
    val titleEn: String,
    val titleHi: String,
    val voiceTriggers: List<String>
) {
    BACK(
        titleEn = "Navigate Back",
        titleHi = "Peeche Jao (Back)",
        voiceTriggers = listOf("peeche jao", "wapas jao", "go back", "back", "piche", "पीछे जाओ", "वापस जाओ")
    ),
    HOME(
        titleEn = "Home Screen",
        titleHi = "Home Screen Par Jao",
        voiceTriggers = listOf("home jao", "go home", "home screen", "main screen", "होम जाओ", "होम स्क्रीन")
    ),
    RECENTS(
        titleEn = "Recent Apps",
        titleHi = "Recent Apps Dikhao",
        voiceTriggers = listOf("recent apps", "recents", "recent dikhao", "khule apps", "रीसेंट ऐप्स")
    ),
    NOTIFICATIONS(
        titleEn = "Notifications Panel",
        titleHi = "Notification Kholo",
        voiceTriggers = listOf("notification dikhao", "open notifications", "notifications", "alert dikhao", "नोटिफिकेशन दिखाओ")
    ),
    QUICK_SETTINGS(
        titleEn = "Quick Settings",
        titleHi = "Control Center Kholo",
        voiceTriggers = listOf("quick settings", "control center", "upari panel", "क्विक सेटिंग्स")
    ),
    SCROLL_DOWN(
        titleEn = "Scroll Down",
        titleHi = "Neeche Scroll Karo",
        voiceTriggers = listOf("neeche scroll karo", "scroll down", "neeche jao", "niche karo", "नीचे स्क्रॉल करो")
    ),
    SCROLL_UP(
        titleEn = "Scroll Up",
        titleHi = "Upar Scroll Karo",
        voiceTriggers = listOf("upar scroll karo", "scroll up", "upar jao", "upar karo", "ऊपर स्क्रॉल करो")
    ),
    SCREENSHOT(
        titleEn = "Take Screenshot",
        titleHi = "Screenshot Lo",
        voiceTriggers = listOf("screenshot lo", "take screenshot", "screen capture", "photo khicho screen ka", "स्क्रीनशॉट लो")
    ),
    LOCK_SCREEN(
        titleEn = "Lock Screen",
        titleHi = "Screen Lock Karo",
        voiceTriggers = listOf("screen lock karo", "lock phone", "phone band karo", "lock screen", "स्क्रीन लॉक करो")
    )
}

class AwaazAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional window state tracking for contextual feedback
        event?.packageName?.toString()?.let { pkg ->
            if (pkg.isNotBlank()) {
                _activePackageName.value = pkg
            }
        }
    }

    override fun onInterrupt() {
        // Handle interruption gracefully
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
            _isConnected.value = false
        }
    }

    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 320L): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun executeAction(actionType: GlobalActionType): Boolean {
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val height = displayMetrics.heightPixels.toFloat()

        return when (actionType) {
            GlobalActionType.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            GlobalActionType.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            GlobalActionType.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            GlobalActionType.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            GlobalActionType.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            GlobalActionType.SCROLL_DOWN -> {
                // Swipe upward to scroll down the content on screen
                performSwipe(centerX, height * 0.75f, centerX, height * 0.25f)
            }
            GlobalActionType.SCROLL_UP -> {
                // Swipe downward to scroll up the content on screen
                performSwipe(centerX, height * 0.25f, centerX, height * 0.75f)
            }
            GlobalActionType.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                } else {
                    false
                }
            }
            GlobalActionType.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else {
                    false
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AwaazAccessibilityService? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _activePackageName = MutableStateFlow("com.example")
        val activePackageName: StateFlow<String> = _activePackageName.asStateFlow()

        fun performSystemAction(actionType: GlobalActionType): Boolean {
            val service = instance ?: return false
            return service.executeAction(actionType)
        }
    }
}
