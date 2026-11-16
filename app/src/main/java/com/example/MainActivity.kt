package com.example

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.assistant.AuraAccessibilityService
import com.example.settings.AssistantSettings
import com.example.settings.VoiceOption
import com.example.ui.AuraMateScreen
import com.example.ui.AuraMateViewModel
import com.example.ui.theme.AuraMateTheme
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var onSpeakingStateChanged: ((Boolean) -> Unit)? = null
    private var onVoicesLoaded: ((List<VoiceOption>) -> Unit)? = null

    private fun launchIntentSafely(candidates: List<Intent>): Boolean {
        for (candidate in candidates) {
            try {
                val resolved = candidate.resolveActivity(packageManager)
                if (resolved != null) {
                    startActivity(candidate)
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch intent candidate: $candidate", e)
            }
        }
        return false
    }

    private fun applyVoiceSettings(settings: AssistantSettings) {
        if (!ttsReady) return
        val engine = tts ?: return
        engine.setPitch(settings.voicePitch)
        engine.setSpeechRate(settings.voiceSpeed)
        val chosenName = settings.selectedVoiceName
        if (!chosenName.isNullOrBlank()) {
            val matched = runCatching { engine.voices?.firstOrNull { it.name == chosenName } }.getOrNull()
            if (matched != null) {
                engine.voice = matched
                return
            }
        }
        val res = engine.setLanguage(Locale.forLanguageTag(settings.voiceLocaleTag))
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.getDefault()
        }
    }

    private fun extractVoiceOptions(voices: Set<Voice>?): List<VoiceOption> {
        if (voices.isNullOrEmpty()) return emptyList()
        val priorityLocales = listOf("hi-in", "en-in", "en-us", "en-gb")
        val filtered = voices
            .filter { voice ->
                val tag = voice.locale.toLanguageTag().lowercase()
                tag.startsWith("hi") || tag.startsWith("en")
            }
            .sortedWith(
                compareBy<Voice>(
                    { voice ->
                        val idx = priorityLocales.indexOf(voice.locale.toLanguageTag().lowercase())
                        if (idx >= 0) idx else 99
                    },
                    { it.isNetworkConnectionRequired },
                    { it.name }
                )
            )
            .take(12)

        val counters = mutableMapOf<String, Int>()
        return filtered.map { voice ->
            val tag = voice.locale.toLanguageTag()
            val regionLabel = when (tag.lowercase()) {
                "hi-in" -> "Hindi India"
                "en-in" -> "English India"
                "en-us" -> "English US"
                "en-gb" -> "English UK"
                else -> voice.locale.displayName
            }
            val num = (counters[regionLabel] ?: 0) + 1
            counters[regionLabel] = num
            val suffix = if (voice.isNetworkConnectionRequired) " (HD)" else ""
            VoiceOption(
                name = voice.name,
                displayLabel = "$regionLabel #$num$suffix",
                localeTag = tag,
                isNetwork = voice.isNetworkConnectionRequired
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // REQUIRED: Configure App Check with launch intent before Compose UI or ViewModels.
        configureAppCheck(this, intent)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                ttsReady = true
                val voiceOptions = runCatching { extractVoiceOptions(tts?.voices) }.getOrDefault(emptyList())
                runOnUiThread { onVoicesLoaded?.invoke(voiceOptions) }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnUiThread { onSpeakingStateChanged?.invoke(true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread { onSpeakingStateChanged?.invoke(false) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        runOnUiThread { onSpeakingStateChanged?.invoke(false) }
                    }
                })
            }
        }

        setContent {
            val vm: AuraMateViewModel = viewModel()
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            LaunchedEffect(uiState.themeOption.isDark) {
                if (uiState.themeOption.isDark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    )
                }
            }

            val syncPermissions: () -> Unit = {
                val contacts = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                val mic = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                val accessibility = AuraAccessibilityService.isServiceEnabled(this@MainActivity)
                vm.updateAllPermissions(
                    contactsGranted = contacts,
                    micGranted = mic,
                    notificationsGranted = notifications,
                    accessibilityGranted = accessibility
                )
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        syncPermissions()
                        vm.refreshInstalledApps()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(Unit) {
                onSpeakingStateChanged = { speaking -> vm.setSpeaking(speaking) }
                onVoicesLoaded = { voices -> vm.updateAvailableVoices(voices) }
                if (ttsReady) {
                    val existing = runCatching { extractVoiceOptions(tts?.voices) }.getOrDefault(emptyList())
                    if (existing.isNotEmpty()) vm.updateAvailableVoices(existing)
                }
                syncPermissions()
            }

            LaunchedEffect(uiState.settings, ttsReady) {
                applyVoiceSettings(uiState.settings)
            }

            val speakText: (String) -> Unit = { raw ->
                if (ttsReady) {
                    applyVoiceSettings(uiState.settings)
                    val cleaned = raw
                        .replace("*", "")
                        .replace("#", "")
                        .replace("`", "")
                        .trim()
                    if (cleaned.isNotEmpty()) {
                        tts?.speak(
                            cleaned,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "am_${UUID.randomUUID()}"
                        )
                    }
                }
            }

            val previewVoice: () -> Unit = {
                val sample = "Namaste! Main ${uiState.assistantName} hoon. Meri nayi awaaz kaisi lag rahi hai?"
                if (ttsReady) {
                    speakText(sample)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Text-to-Speech abhi taiyaar nahi hai.")
                    }
                }
            }

            val contactsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) {
                syncPermissions()
            }

            val multiPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                syncPermissions()
            }

            val grantAllRuntimePermissions: () -> Unit = {
                val toRequest = buildList {
                    add(Manifest.permission.READ_CONTACTS)
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                multiPermissionLauncher.launch(toRequest)
            }

            val openAccessibilitySettings: () -> Unit = {
                val opened = launchIntentSafely(
                    listOf(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        Intent(Settings.ACTION_SETTINGS)
                    )
                )
                if (!opened) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Accessibility settings nahi khul paayi.")
                    }
                }
            }

            val speechLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val spoken = result.data
                        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                    if (!spoken.isNullOrBlank()) {
                        vm.sendPrompt(spoken, ::launchIntentSafely, speakText)
                    }
                }
            }

            val startVoiceInput: () -> Unit = {
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, uiState.voiceLocaleTag)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "${uiState.assistantName} se kuch bhi boliye…")
                }
                try {
                    tts?.stop()
                    vm.setSpeaking(false)
                    speechLauncher.launch(speechIntent)
                } catch (_: ActivityNotFoundException) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            getString(R.string.speech_unavailable)
                        )
                    }
                }
            }

            AuraMateTheme(themeOption = uiState.themeOption) {
                AuraMateScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onInputChanged = vm::onInputChanged,
                    onSend = { prompt -> vm.sendPrompt(prompt, ::launchIntentSafely, speakText) },
                    onStartVoiceInput = startVoiceInput,
                    onToggleAutoSpeak = {
                        val wasEnabled = uiState.isAutoSpeakEnabled
                        vm.toggleAutoSpeak()
                        if (wasEnabled) {
                            tts?.stop()
                            vm.setSpeaking(false)
                        }
                    },
                    onToggleVoiceLocale = vm::toggleVoiceLocale,
                    onSpeakMessage = speakText,
                    onStopSpeaking = {
                        tts?.stop()
                        vm.setSpeaking(false)
                    },
                    onClearChat = {
                        tts?.stop()
                        vm.setSpeaking(false)
                        vm.clearConversation()
                    },
                    onRetryMessage = { id -> vm.retryMessage(id, ::launchIntentSafely, speakText) },
                    onOpenCommandsSheet = { tab -> vm.setCommandsSheetVisible(true, initialTab = tab) },
                    onSelectCommandsTab = vm::setCommandsSheetTab,
                    onCloseCommandsSheet = { vm.setCommandsSheetVisible(false) },
                    onOpenCustomizeSheet = { vm.setCustomizeSheetVisible(true) },
                    onCloseCustomizeSheet = { vm.setCustomizeSheetVisible(false) },
                    onSaveAssistantName = vm::setAssistantName,
                    onSelectTheme = vm::setThemeOption,
                    onSelectVoice = vm::setSelectedVoiceName,
                    onPitchChanged = vm::setVoicePitch,
                    onSpeedChanged = vm::setVoiceSpeed,
                    onPreviewVoice = previewVoice,
                    onLaunchInstalledApp = { app ->
                        vm.launchInstalledApp(app, ::launchIntentSafely, speakText)
                    },
                    onSearchInPlayStore = { query ->
                        vm.searchAppInPlayStore(query, ::launchIntentSafely)
                    },
                    onGrantContactsClick = {
                        contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    onDismissContactsBanner = vm::dismissContactsBanner,
                    onGrantAllRuntimePermissions = grantAllRuntimePermissions,
                    onOpenAccessibilitySettings = openAccessibilitySettings,
                    onDismissAccessibilityBanner = vm::dismissAccessibilityBanner
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }

    override fun onDestroy() {
        onSpeakingStateChanged = null
        onVoicesLoaded = null
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
