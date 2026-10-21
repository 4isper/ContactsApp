package com.m4isper.contactsapp.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m4isper.contactsapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Виджет избранного на главном экране. */
@RunWith(AndroidJUnit4::class)
class ContactsWidgetProviderTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun provider_is_registered_in_the_manifest() {
        val info =
            context.packageManager.getReceiverInfo(
                ComponentName(context, ContactsWidgetProvider::class.java),
                0,
            )

        assertTrue(info.exported.not())
    }

    @Test
    fun layout_has_rows_for_the_favorites() {
        val views = RemoteViews(context.packageName, R.layout.widget_contacts)

        views.setTextViewText(R.id.widget_row_1, "Anna")

        val root = FrameLayout(context)
        val inflated = views.apply(context, root)
        val row = inflated.findViewById<TextView>(R.id.widget_row_1)
        assertNotNull(row)
        assertEquals("Anna", row.text)
    }

    @Test
    fun update_does_not_fail_without_widget_instances() {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ContactsWidgetProvider()

        provider.onUpdate(context, manager, intArrayOf())

        assertEquals(0, manager.getAppWidgetIds(ComponentName(context, ContactsWidgetProvider::class.java)).size)
    }
}
