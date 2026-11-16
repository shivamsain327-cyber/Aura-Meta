package com.example.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AuraAiRepository
import com.example.assistant.ActionCategory
import com.example.assistant.CommandHandler
import com.example.assistant.CommandOutcome
import com.example.assistant.InstalledAppInfo
import com.example.settings.AssistantPreferencesRepository
import com.example.settings.AssistantSettings
import com.example.settings.AuraThemeOption
import com.example.settings.VoiceOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "AuraMateViewModel"

enum class MessageSender {
    USER,
    AURA
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val actionCategory: ActionCategory? = null,
    val actionDetail: String? = null,
    val actionExecuted: Boolean = true,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val retryPrompt: String? = null
)

data class AuraMateUiState(
    val settings: AssistantSettings = AssistantSettings(),
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome_msg",
            sender = MessageSender.AURA,
            text = welcomeGreeting(AssistantSettings.DEFAULT_ASSISTANT_NAME)
        )
    ),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val showCommandsSheet: Boolean = false,
    val commandsSheetTab: Int = 0, // 0 = All Apps, 1 = Hinglish Commands
    val showCustomizeSheet: Boolean = false,
    val showContactsBanner: Boolean = false,
    val showAccessibilityBanner: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasMicPermission: Boolean = false,
    val hasNotificationsPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val availableVoices: List<VoiceOption> = emptyList()
) {
    val isAutoSpeakEnabled: Boolean get() = settings.isAutoSpeakEnabled
    val voiceLocaleTag: String get() = settings.voiceLocaleTag
    val assistantName: String get() = settings.assistantName
    val themeOption: AuraThemeOption get() = settings.themeOption
}

private fun welcomeGreeting(name: String): String {
    return "Namaste! Main $name hoon. Alarm, Timer, WhatsApp, Sabhi Apps, Device Controls, Music ya koi bhi sawaal — ChatGPT ki tarah boliye ya likhiye."
}

class AuraMateViewModel(application: Application) : AndroidViewModel(application) {

    private val commandHandler = CommandHandler(application.applicationContext)
    private val aiRepository = AuraAiRepository()
    private val preferencesRepository = AssistantPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AuraMateUiState())
    val uiState: StateFlow<AuraMateUiState> = _uiState.asStateFlow()

    private var activeAiJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesRepository.settingsFlow.collect { loadedSettings ->
                _uiState.update { state ->
                    val updatedMessages = if (
                        state.messages.size == 1 &&
                        state.messages.first().id == "welcome_msg"
                    ) {
                        listOf(
                            state.messages.first().copy(
                                text = welcomeGreeting(loadedSettings.assistantName)
                            )
                        )
                    } else {
                        state.messages
                    }
                    state.copy(
                        settings = loadedSettings,
                        messages = updatedMessages
                    )
                }
                aiRepository.updatePersonaContext(
                    assistantName = loadedSettings.assistantName,
                    installedAppLabels = _uiState.value.installedApps.map { it.label }
                )
            }
        }
        refreshInstalledApps()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                commandHandler.getInstalledApps()
            }
            _uiState.update { it.copy(installedApps = apps) }
            aiRepository.updatePersonaContext(
                assistantName = _uiState.value.assistantName,
                installedAppLabels = apps.map { it.label }
            )
        }
    }

    fun onInputChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun toggleAutoSpeak() {
        val next = !_uiState.value.isAutoSpeakEnabled
        viewModelScope.launch {
            preferencesRepository.setAutoSpeakEnabled(next)
        }
    }

    fun toggleVoiceLocale() {
        val next = if (_uiState.value.voiceLocaleTag == "hi-IN") "en-IN" else "hi-IN"
        viewModelScope.launch {
            preferencesRepository.setVoiceLocaleTag(next)
        }
    }

    fun setCommandsSheetVisible(visible: Boolean, initialTab: Int? = null) {
        if (visible) {
            refreshInstalledApps()
        }
        _uiState.update { state ->
            state.copy(
                showCommandsSheet = visible,
                commandsSheetTab = initialTab ?: state.commandsSheetTab
            )
        }
    }

    fun setCommandsSheetTab(tab: Int) {
        _uiState.update { it.copy(commandsSheetTab = tab) }
    }

    fun setCustomizeSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showCustomizeSheet = visible) }
    }

    fun updateAvailableVoices(voices: List<VoiceOption>) {
        _uiState.update { it.copy(availableVoices = voices) }
    }

    fun setAssistantName(name: String) {
        viewModelScope.launch {
            preferencesRepository.setAssistantName(name)
        }
    }

    fun setThemeOption(theme: AuraThemeOption) {
        viewModelScope.launch {
            preferencesRepository.setThemeOption(theme)
        }
    }

    fun setSelectedVoiceName(voiceName: String?) {
        viewModelScope.launch {
            preferencesRepository.setSelectedVoiceName(voiceName)
        }
    }

    fun setVoicePitch(pitch: Float) {
        viewModelScope.launch {
            preferencesRepository.setVoicePitch(pitch)
        }
    }

    fun setVoiceSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesRepository.setVoiceSpeed(speed)
        }
    }

    fun updateAllPermissions(
        contactsGranted: Boolean,
        micGranted: Boolean,
        notificationsGranted: Boolean,
        accessibilityGranted: Boolean
    ) {
        _uiState.update { state ->
            state.copy(
                hasContactsPermission = contactsGranted,
                hasMicPermission = micGranted,
                hasNotificationsPermission = notificationsGranted,
                hasAccessibilityPermission = accessibilityGranted,
                showContactsBanner = if (contactsGranted) false else state.showContactsBanner,
                showAccessibilityBanner = if (accessibilityGranted) false else state.showAccessibilityBanner
            )
        }
    }

    fun updateContactsPermission(granted: Boolean) {
        _uiState.update { state ->
            state.copy(
                hasContactsPermission = granted,
                showContactsBanner = if (granted) false else state.showContactsBanner
            )
        }
    }

    fun dismissContactsBanner() {
        _uiState.update { it.copy(showContactsBanner = false) }
    }

    fun dismissAccessibilityBanner() {
        _uiState.update { it.copy(showAccessibilityBanner = false) }
    }

    fun setSpeaking(speaking: Boolean) {
        _uiState.update { it.copy(isSpeaking = speaking) }
    }

    fun searchAppInPlayStore(
        query: String,
        onLaunchIntents: (List<Intent>) -> Boolean
    ) {
        val intents = commandHandler.buildPlayStoreIntents(query)
        onLaunchIntents(intents)
    }

    fun launchInstalledApp(
        app: InstalledAppInfo,
        onLaunchIntents: (List<Intent>) -> Boolean,
        onSpeakReply: (String) -> Unit
    ) {
        val intents = commandHandler.buildLaunchIntentsForInstalledApp(app)
        val launched = onLaunchIntents(intents)
        val reply = if (launched) {
            "${app.label} khol rahi hoon."
        } else {
            "${app.label} kholne mein dikkat aayi."
        }
        val msg = ChatMessage(
            sender = MessageSender.AURA,
            text = reply,
            actionCategory = ActionCategory.OPEN_APP,
            actionDetail = app.label,
            actionExecuted = launched
        )
        _uiState.update { state ->
            state.copy(
                showCommandsSheet = false,
                messages = state.messages + msg
            )
        }
        if (_uiState.value.isAutoSpeakEnabled) {
            onSpeakReply(reply)
        }
    }

    fun cancelActiveAiRequest() {
        activeAiJob?.cancel()
        activeAiJob = null
        _uiState.update { state ->
            state.copy(
                isThinking = false,
                messages = state.messages.map { msg ->
                    if (msg.isStreaming) {
                        msg.copy(
                            text = msg.text.ifBlank { "Request cancel kar di gayi." },
                            isStreaming = false
                        )
                    } else {
                        msg
                    }
                }
            )
        }
    }

    fun clearConversation() {
        activeAiJob?.cancel()
        aiRepository.resetSession()
        val name = _uiState.value.assistantName
        _uiState.update { state ->
            state.copy(
                messages = listOf(
                    ChatMessage(
                        id = "welcome_msg",
                        sender = MessageSender.AURA,
                        text = welcomeGreeting(name)
                    )
                ),
                isThinking = false,
                showContactsBanner = false
            )
        }
    }

    fun sendPrompt(
        rawText: String,
        onLaunchIntents: (List<Intent>) -> Boolean,
        onSpeakReply: (String) -> Unit
    ) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = text)
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg,
                inputText = if (rawText == state.inputText) "" else state.inputText
            )
        }

        // Layer 1: Instant deterministic local command execution
        val directOutcome = commandHandler.handle(text)
        if (directOutcome != null) {
            val msg = executeOutcome(directOutcome, onLaunchIntents)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + msg,
                    showContactsBanner = directOutcome.needsContactsPermission && !state.hasContactsPermission,
                    showAccessibilityBanner = directOutcome.needsAccessibilityPermission && !state.hasAccessibilityPermission
                )
            }
            if (_uiState.value.isAutoSpeakEnabled) {
                onSpeakReply(msg.text)
            }
            return
        }

        // Layer 2: Gemini AI Agent understands natural ChatGPT-style tasks and triggers real Android actions
        activeAiJob?.cancel()
        runAiAgentTurn(
            prompt = text,
            onLaunchIntents = onLaunchIntents,
            onSpeakReply = onSpeakReply
        )
    }

    fun retryMessage(
        messageId: String,
        onLaunchIntents: (List<Intent>) -> Boolean,
        onSpeakReply: (String) -> Unit
    ) {
        val target = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val prompt = target.retryPrompt ?: return

        activeAiJob?.cancel()
        _uiState.update { state ->
            state.copy(messages = state.messages.filterNot { it.id == messageId })
        }
        runAiAgentTurn(
            prompt = prompt,
            onLaunchIntents = onLaunchIntents,
            onSpeakReply = onSpeakReply
        )
    }

    private fun executeOutcome(
        outcome: CommandOutcome,
        onLaunchIntents: (List<Intent>) -> Boolean,
        id: String = UUID.randomUUID().toString()
    ): ChatMessage {
        val launched = if (outcome.pendingIntents.isNotEmpty()) {
            onLaunchIntents(outcome.pendingIntents)
        } else {
            outcome.executed
        }
        val finalReply = if (launched || outcome.pendingIntents.isEmpty()) {
            outcome.reply
        } else {
            outcome.failureReply ?: outcome.reply
        }
        return ChatMessage(
            id = id,
            sender = MessageSender.AURA,
            text = finalReply,
            actionCategory = outcome.category,
            actionDetail = if (launched) outcome.detail else "Unavailable",
            actionExecuted = launched && outcome.executed
        )
    }

    private fun runAiAgentTurn(
        prompt: String,
        onLaunchIntents: (List<Intent>) -> Boolean,
        onSpeakReply: (String) -> Unit
    ) {
        val thinkingId = UUID.randomUUID().toString()
        val placeholder = ChatMessage(
            id = thinkingId,
            sender = MessageSender.AURA,
            text = "",
            isStreaming = true
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.isStreaming } + placeholder,
                isThinking = true
            )
        }

        activeAiJob = viewModelScope.launch {
            try {
                val plan = aiRepository.planAndReply(prompt)
                val outcome = commandHandler.resolveAiPlan(plan)

                val finalMessage = if (outcome != null) {
                    executeOutcome(outcome, onLaunchIntents, id = thinkingId)
                } else {
                    ChatMessage(
                        id = thinkingId,
                        sender = MessageSender.AURA,
                        text = plan.reply,
                        isStreaming = false
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        isThinking = false,
                        showContactsBanner = (outcome?.needsContactsPermission == true && !state.hasContactsPermission) ||
                            state.showContactsBanner,
                        showAccessibilityBanner = (outcome?.needsAccessibilityPermission == true && !state.hasAccessibilityPermission) ||
                            state.showAccessibilityBanner,
                        messages = state.messages.map { msg ->
                            if (msg.id == thinkingId) finalMessage else msg
                        }
                    )
                }
                if (_uiState.value.isAutoSpeakEnabled) {
                    onSpeakReply(finalMessage.text)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini generation timed out", e)
                updateErrorMessage(
                    thinkingId = thinkingId,
                    errorText = "Jawab aane mein samay lag gaya (timeout). Internet check karke dobara try karein.",
                    retryPrompt = prompt
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                val detail = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "AI se judne mein dikkat aayi."
                updateErrorMessage(
                    thinkingId = thinkingId,
                    errorText = "AI response fail hua: $detail",
                    retryPrompt = prompt
                )
            }
        }
    }

    private fun updateErrorMessage(
        thinkingId: String,
        errorText: String,
        retryPrompt: String
    ) {
        _uiState.update { state ->
            state.copy(
                isThinking = false,
                messages = state.messages.map { msg ->
                    if (msg.id == thinkingId) {
                        msg.copy(
                            text = errorText,
                            isStreaming = false,
                            isError = true,
                            retryPrompt = retryPrompt
                        )
                    } else {
                        msg
                    }
                }
            )
        }
    }
}
