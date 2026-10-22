package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.data.security.Pbkdf2PinHasher
import com.m4isper.contactsapp.domain.model.AppError
import com.m4isper.contactsapp.domain.model.AppSettings
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
import com.m4isper.contactsapp.presentation.reminders.ReminderScheduler
import com.m4isper.contactsapp.presentation.settings.SettingsResetCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Аккаунты для выбора аккаунта по умолчанию: в тесте настроек важен только список. */
private class SettingsFakeContactsRepository : com.m4isper.contactsapp.domain.repository.ContactsAccounts {
    override suspend fun getContactAccount(contactId: Long): com.m4isper.contactsapp.domain.model.ContactAccount =
        com.m4isper.contactsapp.domain.model.ContactAccount.Local

    override suspend fun getContactAccounts(): List<com.m4isper.contactsapp.domain.model.ContactAccount> =
        listOf(com.m4isper.contactsapp.domain.model.ContactAccount.Local)
}

/** Хранилище языка в памяти: настройки проверяются без Android-контекста. */
private class FakeAppLanguageStore : AppLanguageStore {
    override var language: String = ""
}

/** Планировщик напоминаний, который не может поставить будильник. */
private class FailingReminderScheduler : ReminderScheduler {
    override fun schedule() = throw IllegalStateException("no alarm")

    override fun cancel() = Unit
}

/** Планировщик напоминаний без AlarmManager: достаточно отметить, что будильник поставлен. */
private class FakeReminderScheduler : ReminderScheduler {
    var scheduled = false

    override fun schedule() {
        scheduled = true
    }

    override fun cancel() {
        scheduled = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeSettingsRepository = FakeSettingsRepository()) =
        SettingsViewModel(
            observeSettingsUseCase = ObserveSettingsUseCase(repository),
            setThemeModeUseCase = SetThemeModeUseCase(repository),
            setSortOrderUseCase = SetSortOrderUseCase(repository),
            setListDensityUseCase = SetListDensityUseCase(repository),
            setFavoritesFirstUseCase = SetFavoritesFirstUseCase(repository),
            setDefaultContactAccountUseCase = SetDefaultContactAccountUseCase(repository),
            getContactAccountsUseCase = GetContactAccountsUseCase(SettingsFakeContactsRepository()),
            setDynamicColorUseCase = SetDynamicColorUseCase(repository),
            setShowPhotosUseCase = SetShowPhotosUseCase(repository),
            setAppLockUseCase = SetAppLockUseCase(repository, Pbkdf2PinHasher()),
            setSecureWindowUseCase = SetSecureWindowUseCase(repository),
            reset =
                SettingsResetCoordinator(
                    resetSettings = ResetSettingsUseCase(repository),
                    languageStore = FakeAppLanguageStore(),
                    reminders =
                        ReminderSettingsCoordinator(
                            setReminders = SetRemindersUseCase(repository),
                            readSettings = ObserveSettingsUseCase(repository),
                            scheduler = FakeReminderScheduler(),
                        ),
                ),
            reminders =
                ReminderSettingsCoordinator(
                    setReminders = SetRemindersUseCase(repository),
                    readSettings = ObserveSettingsUseCase(repository),
                    scheduler = FakeReminderScheduler(),
                ),
            language =
                AppLanguageCoordinator(
                    setLanguage = SetAppLanguageUseCase(repository),
                    languageStore = FakeAppLanguageStore(),
                ),
        )

    @Test
    fun `new settings are saved`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onLanguageSelected("ru")
            viewModel.onDynamicColorChanged(false)
            viewModel.onShowPhotosChanged(false)
            viewModel.onSecureWindowChanged(true)
            viewModel.onAppLockChanged(true, "1234")
            advanceUntilIdle()
            withContext(Dispatchers.Default) {
                withTimeout(5_000) {
                    while (repository.appLocks.isEmpty()) delay(10)
                }
            }

            assertEquals(listOf("ru"), repository.languages)
            assertEquals(listOf(false), repository.dynamicColorValues)
            assertEquals(listOf(false), repository.showPhotosValues)
            assertEquals(listOf(true), repository.secureWindowValues)
            assertEquals(1, repository.appLocks.size)
            assertEquals(true, repository.appLocks.single().first)
            assertTrue(
                repository.appLocks
                    .single()
                    .second
                    .isNotBlank(),
            )
        }

    @Test
    fun `a failed scheduling of the reminder is shown as an error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val viewModel =
                SettingsViewModel(
                    observeSettingsUseCase = ObserveSettingsUseCase(repository),
                    setThemeModeUseCase = SetThemeModeUseCase(repository),
                    setSortOrderUseCase = SetSortOrderUseCase(repository),
                    setListDensityUseCase = SetListDensityUseCase(repository),
                    setFavoritesFirstUseCase = SetFavoritesFirstUseCase(repository),
                    setDefaultContactAccountUseCase = SetDefaultContactAccountUseCase(repository),
                    getContactAccountsUseCase = GetContactAccountsUseCase(SettingsFakeContactsRepository()),
                    setDynamicColorUseCase = SetDynamicColorUseCase(repository),
                    setShowPhotosUseCase = SetShowPhotosUseCase(repository),
                    setAppLockUseCase = SetAppLockUseCase(repository, Pbkdf2PinHasher()),
                    setSecureWindowUseCase = SetSecureWindowUseCase(repository),
                    reset =
                        SettingsResetCoordinator(
                            resetSettings = ResetSettingsUseCase(repository),
                            languageStore = FakeAppLanguageStore(),
                            reminders =
                                ReminderSettingsCoordinator(
                                    setReminders = SetRemindersUseCase(repository),
                                    readSettings = ObserveSettingsUseCase(repository),
                                    scheduler = FailingReminderScheduler(),
                                ),
                        ),
                    reminders =
                        ReminderSettingsCoordinator(
                            setReminders = SetRemindersUseCase(repository),
                            readSettings = ObserveSettingsUseCase(repository),
                            scheduler = FailingReminderScheduler(),
                        ),
                    language =
                        AppLanguageCoordinator(
                            setLanguage = SetAppLanguageUseCase(repository),
                            languageStore = FakeAppLanguageStore(),
                        ),
                )
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onRemindersChanged(true, 3)
            advanceUntilIdle()

            assertEquals(AppError.SCHEDULE_FAILED, viewModel.uiState.value.error)
        }

    @Test
    fun `reset returns the default settings`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository(AppSettings(dynamicColor = false, showPhotos = false))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onResetSettings()
            advanceUntilIdle()

            assertTrue(repository.resetCalled)
            assertEquals(true, viewModel.uiState.value.settings.dynamicColor)
            assertEquals(true, viewModel.uiState.value.settings.showPhotos)
        }

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
class AppSettingsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `theme mode follows stored settings`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository(AppSettings(themeMode = ThemeMode.DARK))
            val viewModel = AppSettingsViewModel(ObserveSettingsUseCase(repository))

            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.settings.value.themeMode)
        }
}
