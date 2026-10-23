package com.m4isper.contactsapp.data.datasource

import android.content.Context
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDataSourceImplTest {
    private fun createDataSource(context: Context): SettingsDataSourceImpl = SettingsDataSourceImpl(context)

    @Test
    fun settings_returnsDefaults() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            val settings = ds.settings.first()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals(ContactSortOrder.BY_NAME, settings.sortOrder)
            assertEquals(ListDensity.COMFORTABLE, settings.listDensity)
            assertTrue(settings.favoritesFirst)
            assertEquals(ContactAccount.Local, settings.lastContactAccount)
            assertEquals(ContactAccount.Local, settings.defaultContactAccount)
            assertEquals("", settings.appLanguage)
            assertTrue(settings.dynamicColor)
            assertTrue(settings.showPhotos)
            assertFalse(settings.appLockEnabled)
            assertEquals("", settings.appLockPinHash)
            assertFalse(settings.secureWindow)
            assertFalse(settings.remindersEnabled)
        }

    @Test
    fun setThemeMode_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setThemeMode(ThemeMode.DARK.name)
            val settings = ds.settings.first()
            assertEquals(ThemeMode.DARK, settings.themeMode)
        }

    @Test
    fun setSortOrder_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setSortOrder(ContactSortOrder.BY_FAMILY.name)
            val settings = ds.settings.first()
            assertEquals(ContactSortOrder.BY_FAMILY, settings.sortOrder)
        }

    @Test
    fun setListDensity_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setListDensity(ListDensity.COMPACT.name)
            val settings = ds.settings.first()
            assertEquals(ListDensity.COMPACT, settings.listDensity)
        }

    @Test
    fun setFavoritesFirst_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setFavoritesFirst(false)
            val settings = ds.settings.first()
            assertFalse(settings.favoritesFirst)
        }

    @Test
    fun setLastContactAccount_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setLastContactAccount(ContactAccount("name", "type"))
            val settings = ds.settings.first()
            assertEquals("name", settings.lastContactAccount.name)
            assertEquals("type", settings.lastContactAccount.type)
        }

    @Test
    fun setDefaultContactAccount_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setDefaultContactAccount(ContactAccount("def", "def"))
            val settings = ds.settings.first()
            assertEquals("def", settings.defaultContactAccount.name)
            assertEquals("def", settings.defaultContactAccount.type)
        }

    @Test
    fun setAppLanguage_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setAppLanguage("en")
            val settings = ds.settings.first()
            assertEquals("en", settings.appLanguage)
        }

    @Test
    fun setDynamicColor_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setDynamicColor(false)
            val settings = ds.settings.first()
            assertFalse(settings.dynamicColor)
        }

    @Test
    fun setShowPhotos_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setShowPhotos(false)
            val settings = ds.settings.first()
            assertFalse(settings.showPhotos)
        }

    @Test
    fun setAppLock_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setAppLock(true, "hash123")
            val settings = ds.settings.first()
            assertTrue(settings.appLockEnabled)
            assertEquals("hash123", settings.appLockPinHash)
        }

    @Test
    fun setSecureWindow_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setSecureWindow(true)
            val settings = ds.settings.first()
            assertTrue(settings.secureWindow)
        }

    @Test
    fun setReminders_persists() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setReminders(false, 0)
            val settings = ds.settings.first()
            assertFalse(settings.remindersEnabled)
            assertEquals(0, settings.reminderLeadDays)
        }

    @Test
    fun resetSettings_clearsAll() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = createDataSource(context)
            ds.setThemeMode(ThemeMode.DARK.name)
            ds.setAppLanguage("en")
            ds.setAppLock(true, "hash")
            ds.resetSettings()
            val settings = ds.settings.first()
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals("", settings.appLanguage)
            assertFalse(settings.appLockEnabled)
            assertEquals("", settings.appLockPinHash)
        }
}
