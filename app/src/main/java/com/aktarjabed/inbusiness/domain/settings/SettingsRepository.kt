package com.aktarjabed.inbusiness.domain.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    // Keys
    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val GSTIN = stringPreferencesKey("gstin")

        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_SENSITIVITY = stringPreferencesKey("ai_sensitivity")
        val AI_AUTO_CORRECT = booleanPreferencesKey("ai_auto_correct")
        val AI_SUGGESTIONS = booleanPreferencesKey("ai_suggestions")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")

        val QUOTA_WARNING = booleanPreferencesKey("quota_warning")
        val INVOICE_REMINDERS = booleanPreferencesKey("invoice_reminders")

        val ANALYTICS = booleanPreferencesKey("analytics")
        val CRASH_REPORTING = booleanPreferencesKey("crash_reporting")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            userName = preferences[PreferencesKeys.USER_NAME] ?: "",
            userEmail = preferences[PreferencesKeys.USER_EMAIL] ?: "",
            businessName = preferences[PreferencesKeys.BUSINESS_NAME] ?: "",
            gstin = preferences[PreferencesKeys.GSTIN] ?: "",

            aiAnomalyDetectionEnabled = preferences[PreferencesKeys.AI_ENABLED] ?: true,
            aiSensitivity = AISensitivity.valueOf(
                preferences[PreferencesKeys.AI_SENSITIVITY] ?: AISensitivity.MEDIUM.name
            ),
            aiAutoCorrectEnabled = preferences[PreferencesKeys.AI_AUTO_CORRECT] ?: false,
            aiSuggestionsEnabled = preferences[PreferencesKeys.AI_SUGGESTIONS] ?: true,

            themeMode = ThemeMode.valueOf(
                preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ),
            useDynamicColors = preferences[PreferencesKeys.DYNAMIC_COLORS] ?: true,

            quotaWarningEnabled = preferences[PreferencesKeys.QUOTA_WARNING] ?: true,
            invoiceRemindersEnabled = preferences[PreferencesKeys.INVOICE_REMINDERS] ?: true,

            analyticsEnabled = preferences[PreferencesKeys.ANALYTICS] ?: true,
            crashReportingEnabled = preferences[PreferencesKeys.CRASH_REPORTING] ?: true
        )
    }

    suspend fun updateUserProfile(name: String, email: String, businessName: String, gstin: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.BUSINESS_NAME] = businessName
            preferences[PreferencesKeys.GSTIN] = gstin
        }
    }

    suspend fun updateAISettings(
        enabled: Boolean,
        sensitivity: AISensitivity,
        autoCorrect: Boolean,
        suggestions: Boolean
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_ENABLED] = enabled
            preferences[PreferencesKeys.AI_SENSITIVITY] = sensitivity.name
            preferences[PreferencesKeys.AI_AUTO_CORRECT] = autoCorrect
            preferences[PreferencesKeys.AI_SUGGESTIONS] = suggestions
        }
    }

    suspend fun updateTheme(mode: ThemeMode, dynamicColors: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
            preferences[PreferencesKeys.DYNAMIC_COLORS] = dynamicColors
        }
    }

    suspend fun updateNotifications(quotaWarning: Boolean, invoiceReminders: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUOTA_WARNING] = quotaWarning
            preferences[PreferencesKeys.INVOICE_REMINDERS] = invoiceReminders
        }
    }

    suspend fun updatePrivacy(analytics: Boolean, crashReporting: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANALYTICS] = analytics
            preferences[PreferencesKeys.CRASH_REPORTING] = crashReporting
        }
    }
}