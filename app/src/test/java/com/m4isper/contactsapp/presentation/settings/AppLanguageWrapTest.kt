package com.m4isper.contactsapp.presentation.settings

import android.app.Application
import android.app.LocaleManager
import android.os.LocaleList
import com.m4isper.contactsapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Обёртка языка — единое решение для окна, виджета и уведомлений: хранилище
 * ниже Android 13, системный выбор языка приложения на Android 13 и выше.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLanguageWrapTest {
    private fun application(): Application = RuntimeEnvironment.getApplication() as Application

    @Test
    fun `the stored language localizes non-activity contexts`() {
        SharedPrefsAppLanguageStore(application()).language = "ru"

        val wrapped = application().withAppLanguage()

        assertEquals("Время напоминания", wrapped.getString(R.string.settings_reminder_time))
    }

    @Test
    fun `without a stored language the context stays untouched`() {
        SharedPrefsAppLanguageStore(application()).language = ""

        val wrapped = application().withAppLanguage()

        assertEquals("Reminder time", wrapped.getString(R.string.settings_reminder_time))
    }

    @Test
    fun `on android 13 the system per-app language wins over the stored one`() {
        SharedPrefsAppLanguageStore(application()).language = "ru"
        val manager = application().getSystemService(LocaleManager::class.java)
        manager.applicationLocales = LocaleList.forLanguageTags("es")

        val wrapped = application().withAppLanguage()

        // Система уже применила свой выбор к контексту: ручная обёртка не нужна.
        assertSame(application(), wrapped)
    }
}
