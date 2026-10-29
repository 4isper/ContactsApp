package com.m4isper.contactsapp.presentation.settings

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.NoopSystemAppLanguage
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.data.security.Pbkdf2PinHasher
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
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
                    system = NoopSystemAppLanguage(),
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
                    system = NoopSystemAppLanguage(),
                ),
        )

    private fun string(resId: Int): String =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(resId)

    /** Раздел напоминаний показывает строки только при реально доставляемых уведомлениях. */
    private fun grantPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    @Test
    fun languageSheetOffersAllShippedLanguages() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("settings_language").performScrollTo().performClick()

        compose.onNodeWithTag("language_system").assertIsDisplayed()
        compose.onNodeWithTag("language_en").assertIsDisplayed()
        compose.onNodeWithTag("language_ru").assertIsDisplayed()
        compose.onNodeWithTag("language_es").assertIsDisplayed()
        compose.onNodeWithTag("language_de").assertIsDisplayed()
        compose.onNodeWithTag("language_fr").assertIsDisplayed()
        compose.onNodeWithTag("language_pt-BR").assertIsDisplayed()
    }

    @Test
    fun reminderTimeRowOpensSheetAndSavesThePickedTime() {
        // Время нестандартное: подтверждение должно сохранить именно выбор, а не дефолт.
        val repository =
            FakeSettingsRepository(
                AppSettings(remindersEnabled = true, reminderTime = ReminderTime(14, 30)),
            )
        grantPostNotifications()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("reminder_time").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_time_sheet").assertIsDisplayed()
        compose.onNodeWithTag("reminder_time_done").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { repository.reminderValues.isNotEmpty() }
        val (enabled, _, time) = repository.reminderValues.single()
        assertEquals(true, enabled)
        assertEquals(ReminderTime(14, 30), time)
        compose.onNodeWithTag("reminder_time_sheet").assertDoesNotExist()
    }

    @Test
    fun showsStoredSettingsAsCurrentValues() {
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

        compose
            .onNodeWithTag(
                "settings_theme_value",
                useUnmergedTree = true,
            ).assertTextEquals(string(R.string.settings_theme_dark))
        // Свёрнутая строка читается TalkBack целиком: название и текущее значение.
        compose.onNodeWithTag("settings_theme").assertTextContains(string(R.string.settings_theme_dark))
        compose
            .onNodeWithTag(
                "settings_sort_value",
                useUnmergedTree = true,
            ).assertTextEquals(string(R.string.settings_sort_by_family))
        compose
            .onNodeWithTag(
                "settings_density_value",
                useUnmergedTree = true,
            ).assertTextEquals(string(R.string.settings_density_compact))
        compose
            .onNodeWithTag(
                "settings_language_value",
                useUnmergedTree = true,
            ).performScrollTo()
            .assertTextEquals(string(R.string.settings_language_system))
        compose.onNodeWithTag("favorites_first_switch").assertIsOff()

        // Вариант в листе выбора помечен выбранным, когда лист открыт.
        compose.onNodeWithTag("settings_theme").performClick()
        compose.onNodeWithTag("theme_DARK").assertIsSelected()
    }

    @Test
    fun showsNewSettingsSections() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("settings_language").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings_sort").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("dynamic_color_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("show_photos_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("favorites_first_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("app_lock_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("app_lock_state").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("secure_window_switch").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("app_version").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings_licenses").performScrollTo().assertIsDisplayed()
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

        // Два нажатия на настройку: строка, затем вариант в листе.
        compose.onNodeWithTag("settings_theme").performClick()
        compose.onNodeWithTag("theme_LIGHT").performClick()
        compose.onNodeWithTag("settings_sort").performScrollTo().performClick()
        compose.onNodeWithTag("sort_BY_FAMILY").performClick()
        compose.onNodeWithTag("settings_density").performScrollTo().performClick()
        compose.onNodeWithTag("density_COMPACT").performClick()
        compose.onNodeWithTag("favorites_first_switch").performScrollTo().performClick()

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

        compose
            .onNodeWithTag(
                "settings_theme_value",
                useUnmergedTree = true,
            ).assertTextEquals(string(R.string.settings_theme_light))
        compose
            .onNodeWithTag(
                "settings_sort_value",
                useUnmergedTree = true,
            ).assertTextEquals(string(R.string.settings_sort_by_family))
        compose
            .onNodeWithTag(
                "settings_density_value",
                useUnmergedTree = true,
            ).assertTextEquals(string(R.string.settings_density_compact))
    }

    @Test
    fun resetSitsInDangerZoneAndAsksForConfirmation() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("reset_settings_button").performScrollTo().performClick()

        compose.onNodeWithTag("confirm_reset_settings").assertIsDisplayed()
        compose.onNodeWithTag("confirm_reset_settings").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { repository.resetCalled }
    }

    @Test
    fun appLockStateShowsEnabledAndOffersChangePin() {
        val repository =
            FakeSettingsRepository(
                AppSettings(appLockEnabled = true, appLockPinHash = "stored-hash"),
            )
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("app_lock_state").performScrollTo()
        compose.onNodeWithTag("app_lock_state").assertTextEquals(string(R.string.settings_app_lock_state_on))
        compose.onNodeWithTag("app_lock_change_pin").performClick()

        compose.onNodeWithTag("app_lock_pin_field").performTextInput("5678")
        compose.onNodeWithTag("app_lock_pin_repeat_field").performTextInput("5678")
        compose.onNodeWithTag("app_lock_pin_confirm").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { repository.appLocks.isNotEmpty() }
        assertEquals(true, repository.appLocks.single().first)
    }

    @Test
    fun licensesActionShowsThirdPartyLibraries() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("settings_licenses").performScrollTo().performClick()

        compose.onNodeWithTag("settings_licenses_sheet").assertIsDisplayed()
        compose.onNodeWithText("Kotlin, Coroutines, Serialization").assertIsDisplayed()
        compose.onNodeWithTag("settings_licenses_close").performClick()
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
