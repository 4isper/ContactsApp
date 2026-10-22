package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.SettingsDataSource
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeSettingsDataSource(
    initial: AppSettings = AppSettings(),
) : SettingsDataSource {
    private val state = MutableStateFlow(initial)

    val themeModes = mutableListOf<String>()
    val sortOrders = mutableListOf<String>()
    val densities = mutableListOf<String>()
    val favoritesFirstValues = mutableListOf<Boolean>()
    val lastContactAccounts = mutableListOf<ContactAccount>()

    override val settings: Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: String) {
        themeModes += mode
    }

    override suspend fun setSortOrder(order: String) {
        sortOrders += order
    }

    override suspend fun setListDensity(density: String) {
        densities += density
    }

    override suspend fun setFavoritesFirst(enabled: Boolean) {
        favoritesFirstValues += enabled
    }

    override suspend fun setDefaultContactAccount(account: ContactAccount) = Unit

    override suspend fun setLastContactAccount(account: ContactAccount) {
        lastContactAccounts += account
    }

    override suspend fun setAppLanguage(language: String) = Unit

    override suspend fun setDynamicColor(enabled: Boolean) = Unit

    override suspend fun setShowPhotos(enabled: Boolean) = Unit

    override suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    ) = Unit

    override suspend fun setSecureWindow(enabled: Boolean) = Unit

    override suspend fun resetSettings() = Unit

    override suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
    ) = Unit
}

class SettingsPortsTest {
    @Test
    fun `observe settings delegates to data source`() =
        runBlocking {
            val expected =
                AppSettings(
                    themeMode = ThemeMode.DARK,
                    sortOrder = ContactSortOrder.BY_FAMILY,
                    listDensity = ListDensity.COMPACT,
                )
            val repository = SettingsReaderImpl(FakeSettingsDataSource(expected))

            assertEquals(expected, repository.observeSettings().first())
        }

    @Test
    fun `set theme mode stores enum name`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()

            AppearanceSettingsImpl(dataSource).setThemeMode(ThemeMode.LIGHT)

            assertEquals(listOf("LIGHT"), dataSource.themeModes)
        }

    @Test
    fun `set sort order stores enum name`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()

            ListSettingsImpl(dataSource).setSortOrder(ContactSortOrder.BY_FAMILY)

            assertEquals(listOf("BY_FAMILY"), dataSource.sortOrders)
        }

    @Test
    fun `set list density stores enum name`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()

            ListSettingsImpl(dataSource).setListDensity(ListDensity.COMPACT)

            assertEquals(listOf("COMPACT"), dataSource.densities)
        }

    @Test
    fun `set favorites first delegates to data source`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()

            ListSettingsImpl(dataSource).setFavoritesFirst(false)

            assertEquals(listOf(false), dataSource.favoritesFirstValues)
        }

    @Test
    fun `set last contact account delegates to data source`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")

            ListSettingsImpl(dataSource).setLastContactAccount(google)

            assertEquals(listOf(google), dataSource.lastContactAccounts)
        }
}
