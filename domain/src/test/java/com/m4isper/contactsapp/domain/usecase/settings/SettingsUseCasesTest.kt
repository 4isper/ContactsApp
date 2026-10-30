package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.repository.settings.AppearanceSettings
import com.m4isper.contactsapp.domain.repository.settings.LanguageSettings
import com.m4isper.contactsapp.domain.repository.settings.ListSettings
import com.m4isper.contactsapp.domain.repository.settings.ReminderSettings
import com.m4isper.contactsapp.domain.repository.settings.SettingsMaintenance
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class FakeAppearanceSettings : AppearanceSettings {
    var themeMode: ThemeMode = ThemeMode.SYSTEM
        private set
    var dynamicColor: Boolean = true
        private set
    var showPhotos: Boolean = true
        private set

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dynamicColor = enabled
    }

    override suspend fun setShowPhotos(enabled: Boolean) {
        showPhotos = enabled
    }
}

private class FakeListSettings : ListSettings {
    var sortOrder: ContactSortOrder = ContactSortOrder.BY_NAME
        private set
    var listDensity: ListDensity = ListDensity.COMFORTABLE
        private set
    var favoritesFirst: Boolean = false
        private set

    override suspend fun setSortOrder(order: ContactSortOrder) {
        sortOrder = order
    }

    override suspend fun setListDensity(density: ListDensity) {
        listDensity = density
    }

    override suspend fun setFavoritesFirst(enabled: Boolean) {
        favoritesFirst = enabled
    }
}

private class FakeReminderSettings : ReminderSettings {
    var enabled: Boolean = true
        private set
    var leadDays: Int = 1
        private set
    var time: ReminderTime = AppSettings.DEFAULT_REMINDER_TIME
        private set

    override suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
        time: ReminderTime,
    ) {
        this.enabled = enabled
        this.leadDays = leadDays
        this.time = time
    }
}

private class FakeLanguageSettings : LanguageSettings {
    var language: String = ""
        private set

    override suspend fun setAppLanguage(language: String) {
        this.language = language
    }
}

private class FakeSettingsMaintenance : SettingsMaintenance {
    var reset: Boolean = false
        private set

    override suspend fun resetSettings() {
        reset = true
    }
}

class SettingsUseCasesTest {
    @Test
    fun setThemeModeUseCase() =
        runBlocking {
            val settings = FakeAppearanceSettings()
            val uc = SetThemeModeUseCase(settings)
            uc(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, settings.themeMode)
        }

    @Test
    fun setDynamicColorUseCase() =
        runBlocking {
            val settings = FakeAppearanceSettings()
            val uc = SetDynamicColorUseCase(settings)
            uc(false)
            assertEquals(false, settings.dynamicColor)
        }

    @Test
    fun setShowPhotosUseCase() =
        runBlocking {
            val settings = FakeAppearanceSettings()
            val uc = SetShowPhotosUseCase(settings)
            uc(false)
            assertEquals(false, settings.showPhotos)
        }

    @Test
    fun setSortOrderUseCase() =
        runBlocking {
            val settings = FakeListSettings()
            val uc = SetSortOrderUseCase(settings)
            uc(ContactSortOrder.BY_FAMILY)
            assertEquals(ContactSortOrder.BY_FAMILY, settings.sortOrder)
        }

    @Test
    fun setListDensityUseCase() =
        runBlocking {
            val settings = FakeListSettings()
            val uc = SetListDensityUseCase(settings)
            uc(ListDensity.COMPACT)
            assertEquals(ListDensity.COMPACT, settings.listDensity)
        }

    @Test
    fun setFavoritesFirstUseCase() =
        runBlocking {
            val settings = FakeListSettings()
            val uc = SetFavoritesFirstUseCase(settings)
            uc(true)
            assertEquals(true, settings.favoritesFirst)
        }

    @Test
    fun setRemindersUseCase() =
        runBlocking {
            val settings = FakeReminderSettings()
            val uc = SetRemindersUseCase(settings)
            uc(false, 0, ReminderTime(8, 15))
            assertEquals(false, settings.enabled)
            assertEquals(0, settings.leadDays)
            assertEquals(ReminderTime(8, 15), settings.time)
        }

    @Test
    fun setSecureWindowUseCase() =
        runBlocking {
            var secureWindow = false
            val settings =
                object : com.m4isper.contactsapp.domain.repository.settings.PrivacySettings {
                    override suspend fun setAppLock(
                        enabled: Boolean,
                        pinHash: String,
                        pinLength: Int,
                    ) = Unit

                    override suspend fun setSecureWindow(enabled: Boolean) {
                        secureWindow = enabled
                    }

                    override suspend fun setAppLockBiometrics(enabled: Boolean) = Unit
                }
            val uc = SetSecureWindowUseCase(settings)
            uc(true)
            assertEquals(true, secureWindow)
        }

    @Test
    fun setAppLanguageUseCase() =
        runBlocking {
            val settings = FakeLanguageSettings()
            val uc = SetAppLanguageUseCase(settings)
            uc("en")
            assertEquals("en", settings.language)
        }

    @Test
    fun resetSettingsUseCase() =
        runBlocking {
            val settings = FakeSettingsMaintenance()
            val uc = ResetSettingsUseCase(settings)
            uc()
            assertEquals(true, settings.reset)
        }
}
