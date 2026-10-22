package com.m4isper.contactsapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.ResetSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.SetAppLanguageUseCase
import com.m4isper.contactsapp.domain.usecase.SetAppLockUseCase
import com.m4isper.contactsapp.domain.usecase.SetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.SetDynamicColorUseCase
import com.m4isper.contactsapp.domain.usecase.SetFavoritesFirstUseCase
import com.m4isper.contactsapp.domain.usecase.SetListDensityUseCase
import com.m4isper.contactsapp.domain.usecase.SetRemindersUseCase
import com.m4isper.contactsapp.domain.usecase.SetSecureWindowUseCase
import com.m4isper.contactsapp.domain.usecase.SetShowPhotosUseCase
import com.m4isper.contactsapp.domain.usecase.SetSortOrderUseCase
import com.m4isper.contactsapp.domain.usecase.SetThemeModeUseCase
import com.m4isper.contactsapp.presentation.common.safeCall
import com.m4isper.contactsapp.presentation.contacts.SUBSCRIBE_TIMEOUT_MILLIS
import com.m4isper.contactsapp.presentation.reminders.ReminderScheduler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SETTINGS_TAG = "SettingsScreen"

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    /** Аккаунты, доступные для новых контактов. */
    val accounts: List<ContactAccount> = emptyList(),
)

// Подавление осознанное: набор настроек растёт, разбивать по файлам неудобно.
@Suppress("TooManyFunctions") // Осознанно: у экрана настроек есть своя функция на каждую настройку.
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class SettingsViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setSortOrderUseCase: SetSortOrderUseCase,
    private val setListDensityUseCase: SetListDensityUseCase,
    private val setFavoritesFirstUseCase: SetFavoritesFirstUseCase,
    private val setDefaultContactAccountUseCase: SetDefaultContactAccountUseCase,
    private val getContactAccountsUseCase: GetContactAccountsUseCase,
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val setDynamicColorUseCase: SetDynamicColorUseCase,
    private val setShowPhotosUseCase: SetShowPhotosUseCase,
    private val setAppLockUseCase: SetAppLockUseCase,
    private val setSecureWindowUseCase: SetSecureWindowUseCase,
    private val resetSettingsUseCase: ResetSettingsUseCase,
    private val setRemindersUseCase: SetRemindersUseCase,
    private val languageStore: AppLanguageStore,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val accountsFlow = MutableStateFlow<List<ContactAccount>>(emptyList())

    val uiState: StateFlow<SettingsUiState> =
        combine(observeSettingsUseCase(), accountsFlow) { settings, accounts ->
            SettingsUiState(settings = settings, accounts = accounts)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    init {
        loadAccounts()
    }

    /** Аккаунты для выбора аккаунта по умолчанию. */
    private fun loadAccounts() {
        viewModelScope.launch {
            safeCall(SETTINGS_TAG, "load accounts failed") {
                accountsFlow.value = getContactAccountsUseCase()
            }.onFailure { failure ->
                accountsFlow.value = listOf(ContactAccount.Local)
            }
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) = update { setThemeModeUseCase(mode) }

    fun onSortOrderSelected(order: ContactSortOrder) = update { setSortOrderUseCase(order) }

    fun onListDensitySelected(density: ListDensity) = update { setListDensityUseCase(density) }

    fun onFavoritesFirstChanged(enabled: Boolean) = update { setFavoritesFirstUseCase(enabled) }

    /** Аккаунт по умолчанию для новых контактов. */
    fun onDefaultAccountSelected(account: ContactAccount) = update { setDefaultContactAccountUseCase(account) }

    /** Язык приложения: сохраняем и в настройках, и в общих настройках — нужен до запуска Activity. */
    fun onLanguageSelected(language: String) =
        update {
            languageStore.language = language
            setAppLanguageUseCase(language)
        }

    /** Динамические цвета Android 12+ вместо фирменной палитры. */
    fun onDynamicColorChanged(enabled: Boolean) = update { setDynamicColorUseCase(enabled) }

    /** Показывать фотографии контактов. */
    fun onShowPhotosChanged(enabled: Boolean) = update { setShowPhotosUseCase(enabled) }

    /** Защита приложения PIN-кодом. */
    fun onAppLockChanged(
        enabled: Boolean,
        pin: String,
    ) = update { setAppLockUseCase(enabled, pin) }

    /** Скрывать содержимое окна в недавних задачах. */
    fun onSecureWindowChanged(enabled: Boolean) = update { setSecureWindowUseCase(enabled) }

    /** Напоминания о днях рождения: включение и планирование будильника. */
    fun onRemindersChanged(
        enabled: Boolean,
        leadDays: Int,
    ) = update {
        setRemindersUseCase(enabled, leadDays)
        if (enabled) {
            reminderScheduler.schedule()
        } else {
            reminderScheduler.cancel()
        }
    }

    /** Вернуть настройки к значениям по умолчанию. */
    fun onResetSettings() =
        update {
            languageStore.language = ""
            resetSettingsUseCase()
            reminderScheduler.cancel()
        }

    // Catch-all осознанный: ошибка записи настроек не должна ронять экран.
    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            safeCall(SETTINGS_TAG, "update settings failed") {
                block()
            }
        }
    }
}
