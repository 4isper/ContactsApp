package com.m4isper.contactsapp.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.data.security.Pbkdf2PinHasher
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
import com.m4isper.contactsapp.presentation.reminders.BirthdayReminderScheduler
import com.m4isper.contactsapp.presentation.settings.AppLanguageCoordinator
import com.m4isper.contactsapp.presentation.settings.ReminderSettingsCoordinator
import com.m4isper.contactsapp.presentation.settings.SettingsResetCoordinator
import com.m4isper.contactsapp.presentation.settings.SharedPrefsAppLanguageStore
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun viewModel(repository: FakeSettingsRepository) =
        SettingsViewModel(
            observeSettingsUseCase = ObserveSettingsUseCase(repository),
            setThemeModeUseCase = SetThemeModeUseCase(repository),
            setSortOrderUseCase = SetSortOrderUseCase(repository),
            setListDensityUseCase = SetListDensityUseCase(repository),
            setFavoritesFirstUseCase = SetFavoritesFirstUseCase(repository),
            setDefaultContactAccountUseCase = SetDefaultContactAccountUseCase(repository),
            getContactAccountsUseCase = GetContactAccountsUseCase(FakeAndroidRepository()),
            setDynamicColorUseCase = SetDynamicColorUseCase(repository),
            setShowPhotosUseCase = SetShowPhotosUseCase(repository),
            setAppLockUseCase = SetAppLockUseCase(repository, Pbkdf2PinHasher()),
            setSecureWindowUseCase = SetSecureWindowUseCase(repository),
            reset =
                SettingsResetCoordinator(
                    resetSettings = ResetSettingsUseCase(repository),
                    languageStore =
                        SharedPrefsAppLanguageStore(
                            InstrumentationRegistry.getInstrumentation().targetContext,
                        ),
                    reminders =
                        ReminderSettingsCoordinator(
                            setReminders = SetRemindersUseCase(repository),
                            readSettings = ObserveSettingsUseCase(repository),
                            scheduler =
                                BirthdayReminderScheduler(
                                    InstrumentationRegistry.getInstrumentation().targetContext,
                                ),
                        ),
                ),
            reminders =
                ReminderSettingsCoordinator(
                    setReminders = SetRemindersUseCase(repository),
                    readSettings = ObserveSettingsUseCase(repository),
                    scheduler =
                        BirthdayReminderScheduler(
                            InstrumentationRegistry.getInstrumentation().targetContext,
                        ),
                ),
            language =
                AppLanguageCoordinator(
                    setLanguage = SetAppLanguageUseCase(repository),
                    languageStore =
                        SharedPrefsAppLanguageStore(
                            InstrumentationRegistry.getInstrumentation().targetContext,
                        ),
                ),
        )

    @Test
    fun showsStoredSettingsSelected() {
        val repository =
            FakeSettingsRepository(
                AppSettings(
                    themeMode = ThemeMode.DARK,
                    sortOrder = ContactSortOrder.BY_FAMILY,
                    listDensity = ListDensity.COMPACT,
                    favoritesFirst = false,
                ),
            )
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("theme_DARK").assertIsSelected()
        compose.onNodeWithTag("sort_BY_FAMILY").assertIsSelected()
        compose.onNodeWithTag("density_COMPACT").assertIsSelected()
        compose.onNodeWithTag("favorites_first_switch").assertIsOff()
    }

    @Test
    fun showsNewSettingsSections() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("language_system").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dynamic_color_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("show_photos_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("app_lock_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("secure_window_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("app_version").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("reset_settings_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun togglingNewSwitchesPersistsThem() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("dynamic_color_switch").performScrollTo().performClick()
        compose.onNodeWithTag("show_photos_switch").performScrollTo().performClick()
        compose.onNodeWithTag("secure_window_switch").performScrollTo().performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            repository.secureWindowValues.isNotEmpty()
        }
        assertEquals(listOf(false), repository.dynamicColorValues)
        assertEquals(listOf(false), repository.showPhotosValues)
        assertEquals(listOf(true), repository.secureWindowValues)
    }

    @Test
    fun appLockAsksForPinBeforeEnabling() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("app_lock_switch").performScrollTo().performClick()

        compose.onNodeWithTag("app_lock_pin_dialog").assertIsDisplayed()
        compose.onNodeWithTag("app_lock_pin_field").performTextInput("1234")
        compose.onNodeWithTag("app_lock_pin_repeat_field").performTextInput("1234")
        compose.onNodeWithTag("app_lock_pin_confirm").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { repository.appLocks.isNotEmpty() }
        assertEquals(true, repository.appLocks.single().first)
    }

    @Test
    fun selectingOptionsPersistsThem() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("theme_LIGHT").performClick()
        compose.onNodeWithTag("sort_BY_FAMILY").performClick()
        compose.onNodeWithTag("density_COMPACT").performClick()
        compose.onNodeWithTag("favorites_first_switch").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            repository.themeModes.isNotEmpty() &&
                repository.sortOrders.isNotEmpty() &&
                repository.densities.isNotEmpty() &&
                repository.favoritesFirstValues.isNotEmpty()
        }
        assertEquals(listOf(ThemeMode.LIGHT), repository.themeModes)
        assertEquals(listOf(ContactSortOrder.BY_FAMILY), repository.sortOrders)
        assertEquals(listOf(ListDensity.COMPACT), repository.densities)
        assertEquals(listOf(false), repository.favoritesFirstValues)
    }

    @Test
    fun backButtonInvokesCallback() {
        val backPressed = AtomicBoolean(false)
        compose.setContent {
            SettingsScreen(
                onBack = { backPressed.set(true) },
                viewModel = viewModel(FakeSettingsRepository()),
            )
        }

        compose.onNodeWithTag("settings_back_button").performClick()

        assertEquals(true, backPressed.get())
    }
}
