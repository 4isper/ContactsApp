package com.m4isper.contactsapp.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
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

/** Неизвестные значения (старая версия, битые данные) откатываются к дефолтам модели. */
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
}

private fun String?.toThemeMode(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

private fun String?.toSortOrder(): ContactSortOrder =
    ContactSortOrder.entries.firstOrNull { it.name == this } ?: ContactSortOrder.BY_NAME

private fun String?.toListDensity(): ListDensity =
    ListDensity.entries.firstOrNull { it.name == this } ?: ListDensity.COMFORTABLE
