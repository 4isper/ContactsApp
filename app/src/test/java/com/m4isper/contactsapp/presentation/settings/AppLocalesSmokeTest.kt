package com.m4isper.contactsapp.presentation.settings

import android.app.Application
import com.m4isper.contactsapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Каждая переведённая локаль действительно рендерит строки: обёртка языка
 * применяется к контексту, а заголовок настроек не остаётся английским.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLocalesSmokeTest {
    private fun application(): Application = RuntimeEnvironment.getApplication() as Application

    @Test
    fun `every shipped locale renders its own settings title`() {
        val translated =
            mapOf(
                "ru" to "Настройки",
                "es" to "Ajustes",
                "de" to "Einstellungen",
                "fr" to "Paramètres",
                "pt-BR" to "Configurações",
            )

        translated.forEach { (tag, title) ->
            SharedPrefsAppLanguageStore(application()).language = tag

            val wrapped = application().withAppLanguage()

            assertEquals("locale $tag", title, wrapped.getString(R.string.settings_title))
            assertEquals("locale $tag", title, wrapped.getString(R.string.settings_menu))
        }
    }

    @Test
    fun `plural rules resolve for every shipped locale`() {
        listOf("ru", "es", "de", "fr", "pt-BR").forEach { tag ->
            SharedPrefsAppLanguageStore(application()).language = tag

            val wrapped = application().withAppLanguage()
            val one = wrapped.resources.getQuantityString(R.plurals.settings_reminder_lead_value, 1, 1)
            val other = wrapped.resources.getQuantityString(R.plurals.settings_reminder_lead_value, 5, 5)

            assertNotEquals("locale $tag", "", one)
            assertNotEquals("locale $tag", "", other)
        }
    }
}
