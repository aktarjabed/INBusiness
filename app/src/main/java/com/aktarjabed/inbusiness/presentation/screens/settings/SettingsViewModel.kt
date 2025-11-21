package com.aktarjabed.inbusiness.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.dao.UserQuotaDao
import com.aktarjabed.inbusiness.domain.settings.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val quotaDao: UserQuotaDao
) : ViewModel() {

    private val currentUserId = "default_user" // TODO: Replace with actual auth

    val userSettings: StateFlow<UserSettings> = settingsRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _quotaInfo = MutableStateFlow<QuotaInfo?>(null)
    val quotaInfo: StateFlow<QuotaInfo?> = _quotaInfo.asStateFlow()

    init {
        loadQuotaInfo()
    }

    private fun loadQuotaInfo() {
        viewModelScope.launch {
            quotaDao.getQuotaFlow(currentUserId).collectLatest { quota ->
                _quotaInfo.value = quota?.let {
                    QuotaInfo(
                        tier = it.tier,
                        dailyUsed = it.dailyUsed,
                        dailyLimit = when (it.tier) {
                            "FREE" -> 2
                            else -> Int.MAX_VALUE
                        },
                        monthlyUsed = it.monthlyUsed,
                        bonusInvoices = it.bonusInvoices
                    )
                }
            }
        }
    }

    fun updateUserProfile(name: String, email: String, businessName: String, gstin: String) {
        viewModelScope.launch {
            settingsRepository.updateUserProfile(name, email, businessName, gstin)
        }
    }

    fun updateAISettings(
        enabled: Boolean,
        sensitivity: AISensitivity,
        autoCorrect: Boolean,
        suggestions: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.updateAISettings(enabled, sensitivity, autoCorrect, suggestions)
        }
    }

    fun updateTheme(mode: ThemeMode, dynamicColors: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTheme(mode, dynamicColors)
        }
    }

    fun updateNotifications(quotaWarning: Boolean, invoiceReminders: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotifications(quotaWarning, invoiceReminders)
        }
    }

    fun updatePrivacy(analytics: Boolean, crashReporting: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePrivacy(analytics, crashReporting)
        }
    }
}

data class QuotaInfo(
    val tier: String,
    val dailyUsed: Int,
    val dailyLimit: Int,
    val monthlyUsed: Int,
    val bonusInvoices: Int
)