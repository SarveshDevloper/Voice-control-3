package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AwaazDatabase
import com.example.data.CommandLogEntity
import com.example.data.VoiceControlRepository
import com.example.data.VoiceMacroEntity
import com.example.domain.CommandExecutionResult
import com.example.domain.PhoneCommandExecutor
import com.example.service.AwaazAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

data class AwaazUiState(
    val isListening: Boolean = false,
    val continuousRadarMode: Boolean = false,
    val voiceFeedbackEnabled: Boolean = true,
    val selectedLanguageTag: String = "en-IN", // Default to en-IN for universal Hinglish + English recognition
    val liveTranscript: String = "",
    val lastResultTitle: String = "Voice Control Ready",
    val lastResultSpokenFeedback: String = "Boliye: \"Open YouTube\", \"Torch chalu karo\", \"Neeche scroll karo\" ya \"Home jao\"",
    val lastResultCategory: String = "System",
    val lastResultSuccess: Boolean = true,
    val soundLevelRms: Float = 0.15f,
    val micSignalDb: Int = 0,
    val isHardwareAudioDetected: Boolean = false,
    val showEmulatorVoicePadModal: Boolean = false,
    val diagnosticStatusText: String = "Mic Ready • Tap Orb or Speak",
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val mediaVolumePercent: Int = 60,
    val isTorchActive: Boolean = false,
    val hasMicPermission: Boolean = false,
    val showAccessibilityPromptBanner: Boolean = false,
    val activeWebUrl: String? = null,
    val activeWebTitle: String = ""
)

class AwaazViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AwaazDatabase.getInstance(application)
    private val repository = VoiceControlRepository(db.voiceControlDao())
    val executor = PhoneCommandExecutor(application)

    private val _uiState = MutableStateFlow(AwaazUiState())
    val uiState: StateFlow<AwaazUiState> = _uiState.asStateFlow()

    val isAccessibilityConnected: StateFlow<Boolean> = AwaazAccessibilityService.isConnected

    val macros: StateFlow<List<VoiceMacroEntity>> = repository.allMacros.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val commandLogs: StateFlow<List<CommandLogEntity>> = repository.recentLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var lastPartialSpeech: String = ""
    private var audioMonitorJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        viewModelScope.launch {
            repository.seedDefaultMacrosIfEmpty()
            refreshTelemetry()
        }
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val hiLocale = Locale.forLanguageTag("hi-IN")
                    val res = textToSpeech?.setLanguage(hiLocale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.language = Locale.US
                    }
                    isTtsReady = true
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun refreshTelemetry() {
        _uiState.update { state ->
            state.copy(
                batteryPercent = executor.getCurrentBatteryPercent(),
                isCharging = executor.getBatteryChargingSafe(),
                mediaVolumePercent = executor.getCurrentMediaVolumePercent(),
                isTorchActive = executor.isTorchOn()
            )
        }
    }

    private fun PhoneCommandExecutor.getBatteryChargingSafe(): Boolean = isBatteryCharging()

    fun updateMicPermissionStatus(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasMicPermission = granted,
                diagnosticStatusText = if (granted) "Mic Permission Granted • Ready" else "Mic Permission Needed"
            )
        }
    }

    fun toggleLanguageTag() {
        _uiState.update { state ->
            val next = if (state.selectedLanguageTag == "en-IN") "hi-IN" else "en-IN"
            state.copy(selectedLanguageTag = next)
        }
    }

    fun toggleVoiceFeedback() {
        _uiState.update { it.copy(voiceFeedbackEnabled = !it.voiceFeedbackEnabled) }
    }

    fun closeActiveWebPlayer() {
        _uiState.update { it.copy(activeWebUrl = null, activeWebTitle = "") }
    }

    fun openEmulatorVoicePadModal() {
        _uiState.update { it.copy(showEmulatorVoicePadModal = true) }
    }

    fun dismissEmulatorVoicePadModal() {
        _uiState.update { it.copy(showEmulatorVoicePadModal = false) }
    }

    fun toggleContinuousRadar(enable: Boolean) {
        _uiState.update { it.copy(continuousRadarMode = enable) }
        if (enable) {
            startVoiceListening()
        } else {
            stopVoiceListening()
        }
    }

    fun buildSystemVoiceRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, _uiState.value.selectedLanguageTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye: \"Open YouTube\", \"Torch chalu karo\"…")
        }
    }

    /**
     * Quick hardware mic check & SpeechRecognizer launch.
     * In browser-based streaming emulators, host laptop audio is often not routed to the container's
     * virtual microphone, or SpeechRecognizer times out with NO_MATCH / ERROR_NETWORK.
     * We detect this immediately and pop up the Interactive Voice Command Pad so the user is never stuck!
     */
    fun startVoiceListening(onFallbackToSystemDialog: (() -> Unit)? = null) {
        mainHandler.post {
            try {
                lastPartialSpeech = ""
                val isRecAvailable = SpeechRecognizer.isRecognitionAvailable(getApplication())

                if (!isRecAvailable) {
                    _uiState.update {
                        it.copy(
                            isListening = false,
                            showEmulatorVoicePadModal = true,
                            diagnosticStatusText = "Emulator Virtual Mic Mode (No Speech Engine in Container)",
                            liveTranscript = "Select or type your voice command below!"
                        )
                    }
                    return@post
                }

                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                    setRecognitionListener(createRecognitionListener(onFallbackToSystemDialog))
                }

                val lang = _uiState.value.selectedLanguageTag
                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                }

                _uiState.update {
                    it.copy(
                        isListening = true,
                        liveTranscript = "Sun raha hoon… Boliye: \"Open YouTube\"",
                        diagnosticStatusText = "Listening on Android Mic ($lang)…",
                        soundLevelRms = 0.45f
                    )
                }
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isListening = false,
                        showEmulatorVoicePadModal = true,
                        diagnosticStatusText = "Virtual Mic Mode Active"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun runHardwareMicTest() {
        audioMonitorJob?.cancel()
        audioMonitorJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 16000
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf <= 0) {
                    _uiState.update {
                        it.copy(
                            diagnosticStatusText = "Virtual Emulator Mic: No hardware audio stream detected.",
                            showEmulatorVoicePadModal = true
                        )
                    }
                    return@launch
                }
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    _uiState.update {
                        it.copy(
                            diagnosticStatusText = "Emulator Virtual Mic Blocked — Use Instant Voice Pad",
                            showEmulatorVoicePadModal = true
                        )
                    }
                    return@launch
                }

                recorder.startRecording()
                val buffer = ShortArray(minBuf / 2)
                var maxDbSeen = 0
                val startTime = System.currentTimeMillis()

                while (isActive && System.currentTimeMillis() - startTime < 2600L) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val db = (rms / 120.0).toInt().coerceIn(0, 100)
                        if (db > maxDbSeen) maxDbSeen = db
                        val normRms = (db / 100f).coerceIn(0.15f, 1.0f)
                        _uiState.update {
                            it.copy(
                                micSignalDb = db,
                                soundLevelRms = normRms,
                                isHardwareAudioDetected = maxDbSeen > 5,
                                diagnosticStatusText = "Testing Mic Hardware… Signal Level: $db%"
                            )
                        }
                    }
                    delay(80)
                }

                recorder.stop()
                recorder.release()

                if (maxDbSeen <= 3) {
                    _uiState.update {
                        it.copy(
                            diagnosticStatusText = "Cloud Emulator has 0% Mic Input (Browser Mic not routed). Opening Instant Voice Pad!",
                            showEmulatorVoicePadModal = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            diagnosticStatusText = "Mic Hardware Working ($maxDbSeen% Peak)! Tap Mic to speak."
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        diagnosticStatusText = "Virtual Mic Mode — Use Instant Voice Pad",
                        showEmulatorVoicePadModal = true
                    )
                }
            }
        }
    }

    fun stopVoiceListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            _uiState.update {
                it.copy(
                    isListening = false,
                    soundLevelRms = 0.12f
                )
            }
        }
    }

    private fun createRecognitionListener(
        onFallbackToSystemDialog: (() -> Unit)?
    ): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _uiState.update {
                    it.copy(
                        isListening = true,
                        liveTranscript = "Mic Ready… Ab Boliye!",
                        diagnosticStatusText = "Mic Open • Speak Now"
                    )
                }
            }

            override fun onBeginningOfSpeech() {
                _uiState.update {
                    it.copy(
                        isListening = true,
                        soundLevelRms = 0.72f,
                        isHardwareAudioDetected = true,
                        diagnosticStatusText = "Receiving Voice Audio…"
                    )
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.15f, 1.0f)
                val dbPercent = ((normalized - 0.15f) * 115f).toInt().coerceIn(0, 100)
                _uiState.update {
                    it.copy(
                        soundLevelRms = normalized,
                        micSignalDb = dbPercent,
                        isHardwareAudioDetected = dbPercent > 8 || it.isHardwareAudioDetected
                    )
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _uiState.update {
                    it.copy(
                        isListening = false,
                        soundLevelRms = 0.15f,
                        diagnosticStatusText = "Processing Spoken Command…"
                    )
                }
            }

            override fun onError(error: Int) {
                _uiState.update { it.copy(isListening = false, soundLevelRms = 0.15f) }

                // 1. If partial speech was heard before timeout, execute it right away!
                val capturedPartial = lastPartialSpeech.trim()
                if (capturedPartial.isNotEmpty()) {
                    lastPartialSpeech = ""
                    executeSpokenOrTypedCommand(capturedPartial)
                    return
                }

                // 2. Explain why audio was not heard and open the Interactive Voice Command Pad
                // so the user in the browser emulator can trigger "Open YouTube" or any command in 1 tap!
                val reason = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "Emulator Mic received 0 audio from browser — Use Voice Command Pad below!"
                    SpeechRecognizer.ERROR_AUDIO ->
                        "Microphone audio stream unavailable in emulator — Use Voice Command Pad!"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Speech network offline — Use Voice Command Pad!"
                    else ->
                        "Cloud Emulator Mic has no audio feed — Select your voice command below!"
                }

                _uiState.update {
                    it.copy(
                        diagnosticStatusText = reason,
                        liveTranscript = "Emulator Mic Silent — Tap any Voice Command on screen!",
                        showEmulatorVoicePadModal = !_uiState.value.continuousRadarMode
                    )
                }

                if (_uiState.value.continuousRadarMode) {
                    mainHandler.postDelayed({
                        if (_uiState.value.continuousRadarMode) {
                            startVoiceListening()
                        }
                    }, 1500L)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val topPhrase = matches?.firstOrNull()?.trim().orEmpty().ifEmpty { lastPartialSpeech.trim() }
                lastPartialSpeech = ""
                if (topPhrase.isNotEmpty()) {
                    _uiState.update {
                        it.copy(diagnosticStatusText = "Voice Heard: \"$topPhrase\"")
                    }
                    executeSpokenOrTypedCommand(topPhrase)
                } else {
                    _uiState.update {
                        it.copy(showEmulatorVoicePadModal = true)
                    }
                }
                if (_uiState.value.continuousRadarMode) {
                    mainHandler.postDelayed({
                        if (_uiState.value.continuousRadarMode) {
                            startVoiceListening()
                        }
                    }, 1500L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    lastPartialSpeech = partial
                    _uiState.update {
                        it.copy(
                            liveTranscript = partial,
                            diagnosticStatusText = "Hearing: \"$partial\""
                        )
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun executeSpokenOrTypedCommand(phrase: String) {
        val cleanPhrase = phrase.trim()
        if (cleanPhrase.isEmpty()) return

        _uiState.update { it.copy(showEmulatorVoicePadModal = false) }

        viewModelScope.launch {
            val currentMacros = repository.getAllMacrosSnapshot()
            val result = executor.parseAndExecute(
                rawPhrase = cleanPhrase,
                customMacros = currentMacros,
                onMacroMatched = { matched ->
                    viewModelScope.launch {
                        repository.incrementMacroUsage(matched.id)
                    }
                }
            )
            applyExecutionResult(cleanPhrase, result)
        }
    }

    fun executeDirectActionCode(actionCode: String, spokenLabel: String) {
        _uiState.update { it.copy(showEmulatorVoicePadModal = false) }
        viewModelScope.launch {
            val result = executor.executeByActionCode(actionCode, spokenLabel)
            applyExecutionResult(spokenLabel, result)
        }
    }

    private suspend fun applyExecutionResult(spokenPhrase: String, result: CommandExecutionResult) {
        repository.logCommand(
            spokenPhrase = spokenPhrase,
            matchedActionTitle = result.actionTitle,
            responseMessage = result.spokenFeedbackHiEn,
            category = result.category,
            isSuccess = result.isSuccess
        )

        refreshTelemetry()

        _uiState.update { state ->
            state.copy(
                liveTranscript = "\"$spokenPhrase\"",
                lastResultTitle = result.actionTitle,
                lastResultSpokenFeedback = result.spokenFeedbackHiEn,
                lastResultCategory = result.category,
                lastResultSuccess = result.isSuccess,
                diagnosticStatusText = "Executed: ${result.actionTitle}",
                showAccessibilityPromptBanner = result.requiresAccessibilitySetup,
                activeWebUrl = result.targetWebUrl ?: state.activeWebUrl,
                activeWebTitle = if (result.targetWebUrl != null) result.actionTitle else state.activeWebTitle
            )
        }

        if (_uiState.value.voiceFeedbackEnabled && isTtsReady) {
            try {
                textToSpeech?.speak(
                    result.spokenFeedbackHiEn,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "awaaz_utt_${System.currentTimeMillis()}"
                )
            } catch (_: Exception) {
            }
        }
    }

    fun addCustomMacro(triggerPhrase: String, actionCode: String, descriptionHi: String, category: String) {
        val cleanTrigger = triggerPhrase.trim()
        if (cleanTrigger.isEmpty()) return
        viewModelScope.launch {
            repository.insertMacro(
                VoiceMacroEntity(
                    triggerPhrase = cleanTrigger,
                    targetActionCode = actionCode,
                    descriptionHi = descriptionHi.ifBlank { "Custom voice shortcut ($actionCode)" },
                    category = category
                )
            )
        }
    }

    fun deleteCustomMacro(id: Int) {
        viewModelScope.launch {
            repository.deleteMacro(id)
        }
    }

    fun clearAllCommandLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioMonitorJob?.cancel()
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }
}
