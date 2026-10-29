package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.repository.settings.AccountSettings
import com.m4isper.contactsapp.domain.repository.settings.AppearanceSettings
import com.m4isper.contactsapp.domain.repository.settings.LanguageSettings
import com.m4isper.contactsapp.domain.repository.settings.ListSettings
import com.m4isper.contactsapp.domain.repository.settings.PrivacySettings
import com.m4isper.contactsapp.domain.repository.settings.ReminderSettings
import com.m4isper.contactsapp.domain.repository.settings.SettingsMaintenance
import com.m4isper.contactsapp.domain.repository.settings.SettingsReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Фейк репозитория настроек: in-memory состояние и запись вызовов для проверок. */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsReader,
    AppearanceSettings,
    ListSettings,
    AccountSettings,
    LanguageSettings,
    PrivacySettings,
    ReminderSettings,
    SettingsMaintenance {
    private val state = MutableStateFlow(initial)

    /** Текущие настройки; тесты могут менять их отсюда, чтобы проверить реакцию UI. */
    val settings: StateFlow<AppSettings> = state

    val themeModes = mutableListOf<ThemeMode>()
    val sortOrders = mutableListOf<ContactSortOrder>()
    val densities = mutableListOf<ListDensity>()
    val favoritesFirstValues = mutableListOf<Boolean>()
    val lastContactAccounts = mutableListOf<ContactAccount>()
    val defaultContactAccounts = mutableListOf<ContactAccount>()
    val languages = mutableListOf<String>()
    val dynamicColorValues = mutableListOf<Boolean>()
    val showPhotosValues = mutableListOf<Boolean>()
    val appLocks = mutableListOf<Pair<Boolean, String>>()
    val secureWindowValues = mutableListOf<Boolean>()
    val reminderValues = mutableListOf<Triple<Boolean, Int, ReminderTime>>()
    var resetCalled = false

    override fun observeSettings(): StateFlow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModes += mode
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setSortOrder(order: ContactSortOrder) {
        sortOrders += order
        state.value = state.value.copy(sortOrder = order)
    }

    override suspend fun setListDensity(density: ListDensity) {
        densities += density
        state.value = state.value.copy(listDensity = density)
    }

    override suspend fun setFavoritesFirst(enabled: Boolean) {
        favoritesFirstValues += enabled
        state.value = state.value.copy(favoritesFirst = enabled)
    }

    override suspend fun setLastContactAccount(account: ContactAccount) {
        lastContactAccounts += account
        state.value = state.value.copy(lastContactAccount = account)
    }

    override suspend fun setDefaultContactAccount(account: ContactAccount) {
        defaultContactAccounts += account
        state.value = state.value.copy(defaultContactAccount = account)
    }

    override suspend fun setAppLanguage(language: String) {
        languages += language
        state.value = state.value.copy(appLanguage = language)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dynamicColorValues += enabled
        state.value = state.value.copy(dynamicColor = enabled)
    }

    override suspend fun setShowPhotos(enabled: Boolean) {
        showPhotosValues += enabled
        state.value = state.value.copy(showPhotos = enabled)
    }

    override suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    ) {
        appLocks += enabled to pinHash
        state.value = state.value.copy(appLockEnabled = enabled, appLockPinHash = pinHash)
    }

    override suspend fun setSecureWindow(enabled: Boolean) {
        secureWindowValues += enabled
        state.value = state.value.copy(secureWindow = enabled)
    }

    override suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
        time: ReminderTime,
    ) {
        reminderValues += Triple(enabled, leadDays, time)
        state.value =
            state.value.copy(remindersEnabled = enabled, reminderLeadDays = leadDays, reminderTime = time)
    }

    override suspend fun resetSettings() {
        resetCalled = true
        state.value = AppSettings()
    }
}
