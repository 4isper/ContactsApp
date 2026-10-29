package com.m4isper.contactsapp.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
)

private val THEME_MODE = stringPreferencesKey("theme_mode")
private val SORT_ORDER = stringPreferencesKey("sort_order")
private val LIST_DENSITY = stringPreferencesKey("list_density")
private val FAVORITES_FIRST = booleanPreferencesKey("favorites_first")
private val LAST_ACCOUNT_NAME = stringPreferencesKey("last_contact_account_name")
private val LAST_ACCOUNT_TYPE = stringPreferencesKey("last_contact_account_type")
private val DEFAULT_ACCOUNT_NAME = stringPreferencesKey("default_contact_account_name")
private val DEFAULT_ACCOUNT_TYPE = stringPreferencesKey("default_contact_account_type")
private val APP_LANGUAGE = stringPreferencesKey("app_language")
private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
private val SHOW_PHOTOS = booleanPreferencesKey("show_photos")
private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
private val APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
private val SECURE_WINDOW = booleanPreferencesKey("secure_window")
private val APP_LOCK_BIOMETRICS = booleanPreferencesKey("app_lock_biometrics")
private val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
private val REMINDER_LEAD_DAYS = intPreferencesKey("reminder_lead_days")
private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
private val REMINDER_MINUTE = intPreferencesKey("reminder_minute")

/**
 * Неизвестные значения (старая версия, битые данные) откатываются к дефолтам модели.
 *
 * Подавление осознанное: по одной маленькой функции на настройку.
 */
@Suppress("TooManyFunctions") // Осознанно: у хранилища настроек есть своя функция на каждую настройку.
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SettingsDataSourceImpl(
    private val context: Context,
) : SettingsDataSource {
    override val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { prefs ->
            AppSettings(
                themeMode = prefs[THEME_MODE].toThemeMode(),
                sortOrder = prefs[SORT_ORDER].toSortOrder(),
                listDensity = prefs[LIST_DENSITY].toListDensity(),
                favoritesFirst = prefs[FAVORITES_FIRST] ?: true,
                lastContactAccount =
                    ContactAccount(
                        name = prefs[LAST_ACCOUNT_NAME],
                        type = prefs[LAST_ACCOUNT_TYPE],
                    ),
                defaultContactAccount =
                    ContactAccount(
                        name = prefs[DEFAULT_ACCOUNT_NAME],
                        type = prefs[DEFAULT_ACCOUNT_TYPE],
                    ),
                appLanguage = prefs[APP_LANGUAGE].orEmpty(),
                dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
                showPhotos = prefs[SHOW_PHOTOS] ?: true,
                appLockEnabled = prefs[APP_LOCK_ENABLED] ?: false,
                appLockPinHash = prefs[APP_LOCK_PIN_HASH].orEmpty(),
                secureWindow = prefs[SECURE_WINDOW] ?: false,
                appLockBiometrics = prefs[APP_LOCK_BIOMETRICS] ?: true,
                remindersEnabled = prefs[REMINDERS_ENABLED] ?: false,
                reminderLeadDays = prefs[REMINDER_LEAD_DAYS] ?: AppSettings.DEFAULT_REMINDER_LEAD_DAYS,
                reminderTime =
                    ReminderTime(
                        hour =
                            prefs[REMINDER_HOUR]?.coerceIn(ReminderTime.MIN_HOUR, ReminderTime.MAX_HOUR)
                                ?: AppSettings.DEFAULT_REMINDER_TIME.hour,
                        minute =
                            prefs[REMINDER_MINUTE]?.coerceIn(ReminderTime.MIN_MINUTE, ReminderTime.MAX_MINUTE)
                                ?: AppSettings.DEFAULT_REMINDER_TIME.minute,
                    ),
            )
        }

    override suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    override suspend fun setSortOrder(order: String) {
        context.settingsDataStore.edit { prefs -> prefs[SORT_ORDER] = order }
    }

    override suspend fun setListDensity(density: String) {
        context.settingsDataStore.edit { prefs -> prefs[LIST_DENSITY] = density }
    }

    override suspend fun setFavoritesFirst(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[FAVORITES_FIRST] = enabled }
    }

    override suspend fun setLastContactAccount(account: ContactAccount) {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(LAST_ACCOUNT_NAME)
            prefs.remove(LAST_ACCOUNT_TYPE)
            if (!account.isLocal) {
                account.name?.let { prefs[LAST_ACCOUNT_NAME] = it }
                account.type?.let { prefs[LAST_ACCOUNT_TYPE] = it }
            }
        }
    }

    override suspend fun setDefaultContactAccount(account: ContactAccount) {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(DEFAULT_ACCOUNT_NAME)
            prefs.remove(DEFAULT_ACCOUNT_TYPE)
            if (!account.isLocal) {
                account.name?.let { prefs[DEFAULT_ACCOUNT_NAME] = it }
                account.type?.let { prefs[DEFAULT_ACCOUNT_TYPE] = it }
            }
        }
    }

    override suspend fun setAppLanguage(language: String) {
        context.settingsDataStore.edit { prefs ->
            if (language.isBlank()) {
                prefs.remove(APP_LANGUAGE)
            } else {
                prefs[APP_LANGUAGE] = language
            }
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setShowPhotos(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[SHOW_PHOTOS] = enabled }
    }

    override suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[APP_LOCK_ENABLED] = enabled
            if (pinHash.isBlank()) {
                prefs.remove(APP_LOCK_PIN_HASH)
            } else {
                prefs[APP_LOCK_PIN_HASH] = pinHash
            }
        }
    }

    override suspend fun setSecureWindow(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[SECURE_WINDOW] = enabled }
    }

    override suspend fun setAppLockBiometrics(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[APP_LOCK_BIOMETRICS] = enabled }
    }

    override suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
        time: ReminderTime,
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[REMINDERS_ENABLED] = enabled
            prefs[REMINDER_LEAD_DAYS] = leadDays
            prefs[REMINDER_HOUR] = time.hour
            prefs[REMINDER_MINUTE] = time.minute
        }
    }

    override suspend fun resetSettings() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(THEME_MODE)
            prefs.remove(SORT_ORDER)
            prefs.remove(LIST_DENSITY)
            prefs.remove(FAVORITES_FIRST)
            prefs.remove(LAST_ACCOUNT_NAME)
            prefs.remove(LAST_ACCOUNT_TYPE)
            prefs.remove(DEFAULT_ACCOUNT_NAME)
            prefs.remove(DEFAULT_ACCOUNT_TYPE)
            prefs.remove(APP_LANGUAGE)
            prefs.remove(DYNAMIC_COLOR)
            prefs.remove(SHOW_PHOTOS)
            prefs.remove(APP_LOCK_ENABLED)
            prefs.remove(APP_LOCK_PIN_HASH)
            prefs.remove(SECURE_WINDOW)
            prefs.remove(APP_LOCK_BIOMETRICS)
            prefs.remove(REMINDERS_ENABLED)
            prefs.remove(REMINDER_LEAD_DAYS)
            prefs.remove(REMINDER_HOUR)
            prefs.remove(REMINDER_MINUTE)
        }
    }
}

private fun String?.toThemeMode(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

private fun String?.toSortOrder(): ContactSortOrder =
    ContactSortOrder.entries.firstOrNull { it.name == this } ?: ContactSortOrder.BY_NAME

private fun String?.toListDensity(): ListDensity =
    ListDensity.entries.firstOrNull { it.name == this } ?: ListDensity.COMFORTABLE
