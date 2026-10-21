package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.SetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.SetFavoritesFirstUseCase
import com.m4isper.contactsapp.domain.usecase.SetListDensityUseCase
import com.m4isper.contactsapp.domain.usecase.SetSortOrderUseCase
import com.m4isper.contactsapp.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** Аккаунты для выбора аккаунта по умолчанию: в тесте настроек важен только список. */
private class SettingsFakeContactsRepository : com.m4isper.contactsapp.domain.repository.ContactsRepository {
    override suspend fun getContacts(): List<com.m4isper.contactsapp.domain.model.ContactDetails> = emptyList()

    override suspend fun getContactDetails(contactId: Long): com.m4isper.contactsapp.domain.model.ContactDetails? = null

    override suspend fun getContactForm(contactId: Long): com.m4isper.contactsapp.domain.model.ContactForm? = null

    override suspend fun createContact(form: com.m4isper.contactsapp.domain.model.ContactForm): Long = 1L

    override suspend fun updateContact(
        contactId: Long,
        form: com.m4isper.contactsapp.domain.model.ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.ContactLink? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeSettingsRepository = FakeSettingsRepository()) =
        SettingsViewModel(
            ObserveSettingsUseCase(repository),
            SetThemeModeUseCase(repository),
            SetSortOrderUseCase(repository),
            SetListDensityUseCase(repository),
            SetFavoritesFirstUseCase(repository),
            SetDefaultContactAccountUseCase(repository),
            GetContactAccountsUseCase(SettingsFakeContactsRepository()),
        )

    @Test
    fun `ui state reflects stored settings`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository(AppSettings(themeMode = ThemeMode.DARK))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.uiState.value.settings.themeMode)
        }

    @Test
    fun `theme selection persists and lands in state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.onThemeModeSelected(ThemeMode.LIGHT)
            advanceUntilIdle()

            assertEquals(listOf(ThemeMode.LIGHT), repository.themeModes)
            assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.settings.themeMode)
        }

    @Test
    fun `sort order selection persists and lands in state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.onSortOrderSelected(ContactSortOrder.BY_FAMILY)
            advanceUntilIdle()

            assertEquals(listOf(ContactSortOrder.BY_FAMILY), repository.sortOrders)
            assertEquals(ContactSortOrder.BY_FAMILY, viewModel.uiState.value.settings.sortOrder)
        }

    @Test
    fun `density selection persists and lands in state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.onListDensitySelected(ListDensity.COMPACT)
            advanceUntilIdle()

            assertEquals(listOf(ListDensity.COMPACT), repository.densities)
            assertEquals(ListDensity.COMPACT, viewModel.uiState.value.settings.listDensity)
        }

    @Test
    fun `favorites first selection persists and lands in state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.onFavoritesFirstChanged(false)
            advanceUntilIdle()

            assertEquals(listOf(false), repository.favoritesFirstValues)
            assertEquals(false, viewModel.uiState.value.settings.favoritesFirst)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `theme mode follows stored settings`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository(AppSettings(themeMode = ThemeMode.DARK))
            val viewModel = ThemeViewModel(ObserveSettingsUseCase(repository))

            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        }
}
