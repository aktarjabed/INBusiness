package com.aktarjabed.inbusiness.domain.settings

data class UserSettings(
    // Profile
    val userName: String = "",
    val userEmail: String = "",
    val businessName: String = "",
    val gstin: String = "",

    // AI Settings
    val aiAnomalyDetectionEnabled: Boolean = true,
    val aiSensitivity: AISensitivity = AISensitivity.MEDIUM,
    val aiAutoCorrectEnabled: Boolean = false,
    val aiSuggestionsEnabled: Boolean = true,

    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true,

    // Notifications
    val quotaWarningEnabled: Boolean = true,
    val invoiceRemindersEnabled: Boolean = true,

    // Privacy
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true,

    // Subscription
    val tier: String = "FREE",
    val quotaRemaining: Int = 2
)

enum class AISensitivity {
    LOW,      // Fewer warnings, only critical issues
    MEDIUM,   // Balanced (default)
    HIGH      // Maximum protection, more warnings
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}