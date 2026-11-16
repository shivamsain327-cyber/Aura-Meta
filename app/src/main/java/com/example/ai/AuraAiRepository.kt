package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AuraAiRepository"

enum class AiActionType {
    NONE,
    PLAY_MEDIA,
    SET_ALARM,
    SET_TIMER,
    OPEN_APP,
    WEB_SEARCH,
    WHATSAPP,
    PHONE_CALL,
    SEND_SMS,
    MAPS_SEARCH,
    OPEN_SETTINGS,
    DEVICE_CONTROL,
    SEND_EMAIL,
    SHARE_TEXT,
    ADD_CALENDAR_EVENT,
    PLAY_STORE_SEARCH
}

data class AiActionPlan(
    val reply: String,
    val action: AiActionType = AiActionType.NONE,
    val query: String? = null,
    val appName: String? = null,
    val contactName: String? = null,
    val messageText: String? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val seconds: Int? = null
)

class AuraAiRepository {

    private var currentAssistantName: String = "AuraMate"
    private var currentAppsSummary: String = ""

    private fun buildSystemPrompt(assistantName: String, appsSummary: String): Content {
        val appsNote = if (appsSummary.isNotBlank()) {
            " Installed launcher apps on this user's phone right now: $appsSummary."
        } else {
            ""
        }
        return content {
            text(
                "You are $assistantName, an intelligent Hinglish & English AI device assistant inside the $assistantName Android app, similar to ChatGPT with full phone & app control. " +
                    "Always identify yourself as $assistantName if asked your name. " +
                    "Match the user's language naturally: reply in friendly Hinglish (Roman script unless they use Devanagari) when they use Hinglish/Hindi, or crisp English when they use English. " +
                    "Keep replies concise (1-3 short sentences) with no markdown tables or asterisks so they sound great via Text-to-Speech. " +
                    "CRITICAL ACTION RULE: You control real Android actions via the JSON `action` field! " +
                    "Whenever the user gives ANY task (explicit or indirect/conversational) — such as playing a song/music/video, opening an app (including WhatsApp Business, Instagram, Telegram, etc.), installing/finding an app on Play Store (`PLAY_STORE_SEARCH`), controlling the device (`DEVICE_CONTROL` with `query` = `flashlight_on`, `flashlight_off`, `volume_up`, `volume_down`, `volume_mute`, `volume_max`, `back`, `home`, `recents`, `notifications`, `quick_settings`, `scroll_down`, `scroll_up`, `click:<button text>`, or `type:<text>`), setting an alarm/timer, calling/texting/WhatsApping/emailing someone, sharing text, adding a calendar event, searching the web, finding a place on Maps, or opening Camera/WiFi/Bluetooth/Settings — you MUST set the matching `action` and parameters, NOT `NONE`. " +
                    "For `PLAY_MEDIA`, if the user asks you to pick a song, choose the exact song & artist name in `query`. " +
                    "Never claim in `reply` that you are executing a device action while leaving `action` as `NONE`. Only use `NONE` for pure conversation or Q&A." +
                    appsNote
            )
        }
    }

    private val actionSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(
                description = "Conversational Hinglish or English reply to show and speak to the user."
            ),
            "action" to Schema.enumeration(
                values = AiActionType.entries.map { it.name },
                description = "The Android device action to execute, or NONE for pure conversation."
            ),
            "query" to Schema.string(
                description = "Song/video for PLAY_MEDIA, search terms for WEB_SEARCH/PLAY_STORE_SEARCH, place for MAPS_SEARCH, setting (wifi/bluetooth/camera/general) for OPEN_SETTINGS, control command (flashlight_on/flashlight_off/volume_up/volume_down/volume_mute/volume_max/back/home/recents/notifications/quick_settings/scroll_down/scroll_up/click:<label>/type:<text>) for DEVICE_CONTROL, email address for SEND_EMAIL, event title for ADD_CALENDAR_EVENT, or phone number for PHONE_CALL.",
                nullable = true
            ),
            "appName" to Schema.string(
                description = "Target app name for OPEN_APP, WHATSAPP (e.g. WhatsApp or WhatsApp Business), PLAY_MEDIA, or email subject for SEND_EMAIL.",
                nullable = true
            ),
            "contactName" to Schema.string(
                description = "Recipient contact name for WHATSAPP, PHONE_CALL, or SEND_SMS.",
                nullable = true
            ),
            "messageText" to Schema.string(
                description = "Message body for WHATSAPP or SEND_SMS.",
                nullable = true
            ),
            "hour" to Schema.integer(
                description = "24-hour value (0-23) for SET_ALARM.",
                nullable = true
            ),
            "minute" to Schema.integer(
                description = "Minute value (0-59) for SET_ALARM.",
                nullable = true
            ),
            "seconds" to Schema.integer(
                description = "Duration in seconds for SET_TIMER.",
                nullable = true
            )
        ),
        optionalProperties = listOf(
            "query",
            "appName",
            "contactName",
            "messageText",
            "hour",
            "minute",
            "seconds"
        )
    )

    private val jsonConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = actionSchema
    }

    private var primaryModel: GenerativeModel? = null
    private var fallbackModel: GenerativeModel? = null

    private fun getPrimaryModel(): GenerativeModel {
        return primaryModel ?: createModel("gemini-3.8-flash").also { primaryModel = it }
    }

    private fun getFallbackModel(): GenerativeModel {
        return fallbackModel ?: createModel("gemini-3.5-flash-lite").also { fallbackModel = it }
    }

    private fun createModel(modelName: String): GenerativeModel {
        return Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            generationConfig = jsonConfig,
            systemInstruction = buildSystemPrompt(currentAssistantName, currentAppsSummary)
        )
    }

    private val history = mutableListOf<Content>()

    fun updatePersonaContext(assistantName: String, installedAppLabels: List<String>) {
        val cleanName = assistantName.trim().ifEmpty { "AuraMate" }
        val summary = installedAppLabels.take(40).joinToString(", ")
        if (cleanName != currentAssistantName || summary != currentAppsSummary) {
            currentAssistantName = cleanName
            currentAppsSummary = summary
            primaryModel = null
            fallbackModel = null
        }
    }

    fun resetSession() {
        history.clear()
    }

    suspend fun planAndReply(prompt: String): AiActionPlan {
        val snapshotHistory = history.toList()

        val primaryRaw = tryUnary(getPrimaryModel(), snapshotHistory, prompt)
        val rawJson = if (!primaryRaw.isNullOrBlank()) {
            primaryRaw
        } else {
            val fallbackResponse = withContext(Dispatchers.IO) {
                withTimeout(30_000L) {
                    getFallbackModel().startChat(history = snapshotHistory).sendMessage(prompt)
                }
            }
            fallbackResponse.text?.trim().orEmpty()
        }

        if (rawJson.isBlank()) {
            throw IllegalStateException("Gemini ne khali jawab bheja. Dobara koshish karein.")
        }

        val plan = parseActionPlan(rawJson)
        recordTurn(prompt, rawJson)
        return plan
    }

    private suspend fun tryUnary(
        model: GenerativeModel,
        snapshotHistory: List<Content>,
        prompt: String
    ): String? {
        return try {
            val response = withContext(Dispatchers.IO) {
                withTimeout(30_000L) {
                    model.startChat(history = snapshotHistory).sendMessage(prompt)
                }
            }
            response.text?.trim()?.ifEmpty { null }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary gemini-3.8-flash timed out; falling back to flash-lite", e)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Primary gemini-3.8-flash failed", e)
            null
        }
    }

    private fun parseActionPlan(jsonText: String): AiActionPlan {
        val cleaned = jsonText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val obj = runCatching { JSONObject(cleaned) }.getOrElse {
            return AiActionPlan(
                reply = cleaned.ifEmpty { "Theek hai!" },
                action = AiActionType.NONE
            )
        }
        val reply = obj.optString("reply").trim()
        val actionStr = obj.optString("action", "NONE").uppercase()
        val action = runCatching { AiActionType.valueOf(actionStr) }
            .getOrDefault(AiActionType.NONE)

        fun optNullableString(key: String): String? {
            if (!obj.has(key) || obj.isNull(key)) return null
            return obj.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
        }

        fun optNullableInt(key: String): Int? {
            if (!obj.has(key) || obj.isNull(key)) return null
            return obj.optInt(key)
        }

        return AiActionPlan(
            reply = reply.ifEmpty { "Theek hai!" },
            action = action,
            query = optNullableString("query"),
            appName = optNullableString("appName"),
            contactName = optNullableString("contactName"),
            messageText = optNullableString("messageText"),
            hour = optNullableInt("hour"),
            minute = optNullableInt("minute"),
            seconds = optNullableInt("seconds")
        )
    }

    private fun recordTurn(userPrompt: String, modelJsonReply: String) {
        history.add(content("user") { text(userPrompt) })
        history.add(content("model") { text(modelJsonReply) })
    }
}
