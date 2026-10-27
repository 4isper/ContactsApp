package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.repository.settings.AccountSettings
import com.m4isper.contactsapp.domain.repository.settings.AppearanceSettings
import com.m4isper.contactsapp.domain.repository.settings.ListSettings
import com.m4isper.contactsapp.domain.repository.settings.SettingsReader
import com.m4isper.contactsapp.domain.usecase.contacts.GetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.SetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetFavoritesFirstUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetListDensityUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetSortOrderUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetThemeModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class StubSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsReader,
    AppearanceSettings,
    ListSettings,
    AccountSettings {
    private val state = MutableStateFlow(initial)

    val themeModes = mutableListOf<ThemeMode>()
    val sortOrders = mutableListOf<ContactSortOrder>()
    val densities = mutableListOf<ListDensity>()
    val favoritesFirstValues = mutableListOf<Boolean>()
    val lastContactAccounts = mutableListOf<ContactAccount>()

    override fun observeSettings(): Flow<AppSettings> = state

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

    override suspend fun setDefaultContactAccount(account: ContactAccount) = Unit

    override suspend fun setDynamicColor(enabled: Boolean) = Unit

    override suspend fun setShowPhotos(enabled: Boolean) = Unit
}

class SettingsUseCasesTest {
    @Test
    fun `observe settings returns repository flow`() =
        runTest {
            val expected =
                AppSettings(
                    themeMode = ThemeMode.DARK,
                    sortOrder = ContactSortOrder.BY_FAMILY,
                    listDensity = ListDensity.COMPACT,
                )
            val repository = StubSettingsRepository(expected)

            assertEquals(expected, ObserveSettingsUseCase(repository)().first())
        }

    @Test
    fun `set theme mode delegates to repository`() =
        runTest {
            val repository = StubSettingsRepository()

            SetThemeModeUseCase(repository)(ThemeMode.LIGHT)

            assertEquals(listOf(ThemeMode.LIGHT), repository.themeModes)
        }

    @Test
    fun `set sort order delegates to repository`() =
        runTest {
            val repository = StubSettingsRepository()

            SetSortOrderUseCase(repository)(ContactSortOrder.BY_FAMILY)

            assertEquals(listOf(ContactSortOrder.BY_FAMILY), repository.sortOrders)
        }

    @Test
    fun `set list density delegates to repository`() =
        runTest {
            val repository = StubSettingsRepository()

            SetListDensityUseCase(repository)(ListDensity.COMPACT)

            assertEquals(listOf(ListDensity.COMPACT), repository.densities)
        }

    @Test
    fun `set favorites first delegates to repository`() =
        runTest {
            val repository = StubSettingsRepository()

            SetFavoritesFirstUseCase(repository)(false)

            assertEquals(listOf(false), repository.favoritesFirstValues)
        }

    @Test
    fun `get last contact account returns saved account`() =
        runTest {
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val repository = StubSettingsRepository(AppSettings(lastContactAccount = google))

            assertEquals(google, GetLastContactAccountUseCase(repository)())
        }

    @Test
    fun `set last contact account delegates to repository`() =
        runTest {
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val repository = StubSettingsRepository()

            SetLastContactAccountUseCase(repository)(google)

            assertEquals(listOf(google), repository.lastContactAccounts)
        }
}
