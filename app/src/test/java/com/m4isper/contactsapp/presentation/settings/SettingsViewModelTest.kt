package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.data.security.Pbkdf2PinHasher
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.SetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.ResetSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetAppLanguageUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetAppLockUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetDynamicColorUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetFavoritesFirstUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetListDensityUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetRemindersUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetSecureWindowUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetShowPhotosUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetSortOrderUseCase
import com.m4isper.contactsapp.domain.usecase.settings.SetThemeModeUseCase
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
    override suspend fun getContactAccount(
        contactId: Long,
    ): com.m4isper.contactsapp.domain.model.contact.ContactAccount =
        com.m4isper.contactsapp.domain.model.contact.ContactAccount.Local

    override suspend fun getContactAccounts(): List<com.m4isper.contactsapp.domain.model.contact.ContactAccount> =
        listOf(com.m4isper.contactsapp.domain.model.contact.ContactAccount.Local)

    override suspend fun getAccountSyncInfos(): List<AccountSyncInfo> = emptyList()
}

/** Хранилище языка в памяти: настройки проверяются без Android-контекста. */
private class FakeAppLanguageStore : AppLanguageStore {
    override var language: String = ""
}

/** Системный выбор языка в памяти: зеркало проверяется без LocaleManager. */
private class FakeSystemAppLanguage : SystemAppLanguage {
    var language: String = ""

    override fun current(): String = language

    override fun set(language: String) {
        this.language = language
    }
}

/** Планировщик напоминаний, который не может поставить будильник. */
private class FailingReminderScheduler : ReminderScheduler {
    override fun schedule(at: ReminderTime) = throw IllegalStateException("no alarm")

    override fun cancel() = Unit
}

/** Планировщик напоминаний без AlarmManager: достаточно отметить, что будильник поставлен. */
private class FakeReminderScheduler : ReminderScheduler {
    var scheduled = false

    override fun schedule(at: ReminderTime) {
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

    private fun viewModel(
        repository: FakeSettingsRepository = FakeSettingsRepository(),
        system: SystemAppLanguage = FakeSystemAppLanguage(),
    ) = SettingsViewModel(
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
                system = system,
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
                system = system,
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
    fun `the chosen language is mirrored into the system`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val system = FakeSystemAppLanguage()
            val viewModel = viewModel(repository, system)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onLanguageSelected("es")
            advanceUntilIdle()

            assertEquals("es", system.language)
            assertEquals(listOf("es"), repository.languages)
        }

    @Test
    fun `reset clears the system language mirror`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeSettingsRepository()
            val system = FakeSystemAppLanguage()
            system.language = "de"
            val viewModel = viewModel(repository, system)

            viewModel.onResetSettings()
            advanceUntilIdle()

            // Иначе после сброса система вернула бы сброшенный язык обратно.
            assertEquals("", system.language)
            assertEquals(true, repository.resetCalled)
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
                            system = FakeSystemAppLanguage(),
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
                            system = FakeSystemAppLanguage(),
                        ),
                )
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onRemindersChanged(true, 3, ReminderTime(9, 0))
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
