package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Настройки лежат в локальном хранилище: записанное значение должно читаться
 * обратно, а сброс возвращать значения по умолчанию.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDataSourceTest {
    private lateinit var dataSource: SettingsDataSourceImpl

    @Before
    fun setUp() {
        dataSource = SettingsDataSourceImpl(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `the theme is stored and read back`() =
        runTest {
            dataSource.setThemeMode(ThemeMode.DARK.name)

            assertEquals(ThemeMode.DARK, dataSource.settings.first().themeMode)
        }

    @Test
    fun `the sort order and the density are stored`() =
        runTest {
            dataSource.setSortOrder(ContactSortOrder.BY_FAMILY.name)
            dataSource.setListDensity(ListDensity.COMPACT.name)

            val settings = dataSource.settings.first()
            assertEquals(ContactSortOrder.BY_FAMILY, settings.sortOrder)
            assertEquals(ListDensity.COMPACT, settings.listDensity)
        }

    @Test
    fun `the account of the last contact is stored`() =
        runTest {
            val account = ContactAccount(name = "user@gmail.com", type = "com.google")

            dataSource.setLastContactAccount(account)

            assertEquals(account, dataSource.settings.first().lastContactAccount)
        }

    @Test
    fun `the privacy settings are stored`() =
        runTest {
            dataSource.setAppLock(enabled = true, pinHash = "hash", pinLength = 6)
            dataSource.setSecureWindow(enabled = true)
            dataSource.setAppLockBiometrics(enabled = false)

            val settings = dataSource.settings.first()
            assertTrue(settings.appLockEnabled)
            assertEquals("hash", settings.appLockPinHash)
            assertEquals(6, settings.appLockPinLength)
            assertTrue(settings.secureWindow)
            assertFalse(settings.appLockBiometrics)
        }

    @Test
    fun `the pin length defaults to unknown`() =
        runTest {
            // Хранилище одно на процесс: сброс гарантирует, что ключей нет.
            dataSource.resetSettings()

            assertEquals(0, dataSource.settings.first().appLockPinLength)
        }

    @Test
    fun `the biometric unlock defaults to on`() =
        runTest {
            // Хранилище одно на процесс: сброс гарантирует, что ключей нет.
            dataSource.resetSettings()

            assertEquals(true, dataSource.settings.first().appLockBiometrics)
        }

    @Test
    fun `the reminders are stored`() =
        runTest {
            dataSource.setReminders(enabled = true, leadDays = 7, time = ReminderTime(9, 0))

            val settings = dataSource.settings.first()
            assertTrue(settings.remindersEnabled)
            assertEquals(7, settings.reminderLeadDays)
        }

    @Test
    fun `the reminder time is stored and read back`() =
        runTest {
            dataSource.setReminders(enabled = true, leadDays = 7, time = ReminderTime(7, 30))

            val settings = dataSource.settings.first()

            assertTrue(settings.remindersEnabled)
            assertEquals(ReminderTime(7, 30), settings.reminderTime)
        }

    @Test
    fun `without stored keys the reminder time falls back to nine in the morning`() =
        runTest {
            // Хранилище одно на процесс: сброс гарантирует, что ключей нет.
            dataSource.resetSettings()

            assertEquals(ReminderTime(9, 0), dataSource.settings.first().reminderTime)
        }

    @Test
    fun `the reset returns the default settings`() =
        runTest {
            dataSource.setThemeMode(ThemeMode.DARK.name)
            dataSource.setShowPhotos(enabled = false)

            dataSource.resetSettings()

            val settings = dataSource.settings.first()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertTrue(settings.showPhotos)
        }
}
