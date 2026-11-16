package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.assistantDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auramate_settings"
)

enum class AuraThemeOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val isDark: Boolean
) {
    OBSIDIAN_SAFFRON("obsidian_saffron", "Obsidian Saffron", "Dark • Gold & Indigo", true),
    MIDNIGHT_EMERALD("midnight_emerald", "Midnight Emerald", "Dark • Cyber Mint", true),
    ROYAL_AMETHYST("royal_amethyst", "Royal Amethyst", "Dark • Violet Nebula", true),
    CRIMSON_VELVET("crimson_velvet", "Crimson Velvet", "Dark • Sunset Rose", true),
    WARM_ALABASTER("warm_alabaster", "Warm Alabaster", "Light • Daylight Ivory", false);

    companion object {
        fun fromId(id: String?): AuraThemeOption {
            return entries.firstOrNull { it.id == id } ?: OBSIDIAN_SAFFRON
        }
    }
}

data class VoiceOption(
    val name: String,
    val displayLabel: String,
    val localeTag: String,
    val isNetwork: Boolean
)

data class AssistantSettings(
    val assistantName: String = DEFAULT_ASSISTANT_NAME,
    val themeOption: AuraThemeOption = AuraThemeOption.OBSIDIAN_SAFFRON,
    val selectedVoiceName: String? = null,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,
    val isAutoSpeakEnabled: Boolean = true,
    val voiceLocaleTag: String = "hi-IN"
) {
    companion object {
        const val DEFAULT_ASSISTANT_NAME = "AuraMate"
    }
}

class AssistantPreferencesRepository(private val context: Context) {

    private object Keys {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val THEME_ID = stringPreferencesKey("theme_id")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val VOICE_LOCALE_TAG = stringPreferencesKey("voice_locale_tag")
    }

    val settingsFlow: Flow<AssistantSettings> = context.assistantDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            AssistantSettings(
                assistantName = prefs[Keys.ASSISTANT_NAME]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: AssistantSettings.DEFAULT_ASSISTANT_NAME,
                themeOption = AuraThemeOption.fromId(prefs[Keys.THEME_ID]),
                selectedVoiceName = prefs[Keys.VOICE_NAME]?.takeIf { it.isNotBlank() },
                voicePitch = (prefs[Keys.VOICE_PITCH] ?: 1.0f).coerceIn(0.6f, 1.4f),
                voiceSpeed = (prefs[Keys.VOICE_SPEED] ?: 1.0f).coerceIn(0.7f, 1.5f),
                isAutoSpeakEnabled = prefs[Keys.AUTO_SPEAK] ?: true,
                voiceLocaleTag = prefs[Keys.VOICE_LOCALE_TAG] ?: "hi-IN"
            )
        }

    suspend fun setAssistantName(name: String) {
        val sanitized = name.trim().take(24).ifEmpty { AssistantSettings.DEFAULT_ASSISTANT_NAME }
        context.assistantDataStore.edit { prefs ->
            prefs[Keys.ASSISTANT_NAME] = sanitized
        }
    }

    suspend fun setThemeOption(theme: AuraThemeOption) {
        context.assistantDataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = theme.id
        }
    }

    suspend fun setSelectedVoiceName(voiceName: String?) {
        context.assistantDataStore.edit { prefs ->
            if (voiceName.isNullOrBlank()) {
                prefs.remove(Keys.VOICE_NAME)
            } else {
                prefs[Keys.VOICE_NAME] = voiceName
            }
        }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.assistantDataStore.edit { prefs ->
            prefs[Keys.VOICE_PITCH] = pitch.coerceIn(0.6f, 1.4f)
        }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.assistantDataStore.edit { prefs ->
            prefs[Keys.VOICE_SPEED] = speed.coerceIn(0.7f, 1.5f)
        }
    }

    suspend fun setAutoSpeakEnabled(enabled: Boolean) {
        context.assistantDataStore.edit { prefs ->
            prefs[Keys.AUTO_SPEAK] = enabled
        }
    }

    suspend fun setVoiceLocaleTag(localeTag: String) {
        context.assistantDataStore.edit { prefs ->
            prefs[Keys.VOICE_LOCALE_TAG] = localeTag
        }
    }
}
