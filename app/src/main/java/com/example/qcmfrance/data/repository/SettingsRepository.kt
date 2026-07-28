package com.example.qcmfrance.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.qcmfrance.data.model.ExamMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class TextSizeMode { SMALL, MEDIUM, LARGE }

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY     = stringPreferencesKey("theme_mode")
    private val SOUND_KEY     = booleanPreferencesKey("sound_enabled")
    private val TEXT_SIZE_KEY = stringPreferencesKey("text_size_mode")
    private val EXAM_MODE_KEY = stringPreferencesKey("exam_mode")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            runCatching { ThemeMode.valueOf(prefs[THEME_KEY] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM)
        }

    val soundEnabled: Flow<Boolean> = context.settingsDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[SOUND_KEY] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[THEME_KEY] = mode.name }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[SOUND_KEY] = enabled }
    }

    val textSizeMode: Flow<TextSizeMode> = context.settingsDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            runCatching { TextSizeMode.valueOf(prefs[TEXT_SIZE_KEY] ?: TextSizeMode.MEDIUM.name) }
                .getOrDefault(TextSizeMode.MEDIUM)
        }

    suspend fun setTextSizeMode(mode: TextSizeMode) {
        context.settingsDataStore.edit { prefs -> prefs[TEXT_SIZE_KEY] = mode.name }
    }

    /** QCM sélectionné sur l'accueil (naturalisation par défaut), persisté entre deux lancements. */
    val examMode: Flow<ExamMode> = context.settingsDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> ExamMode.fromCode(prefs[EXAM_MODE_KEY]) }

    suspend fun setExamMode(mode: ExamMode) {
        context.settingsDataStore.edit { prefs -> prefs[EXAM_MODE_KEY] = mode.code }
    }
}
