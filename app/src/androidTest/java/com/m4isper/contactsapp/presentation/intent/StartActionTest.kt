package com.m4isper.contactsapp.presentation.intent

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Разбор системного intent: приложение должно понимать команды других программ. */
@RunWith(AndroidJUnit4::class)
class StartActionTest {
    private val resolver
        get() = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

    private fun contactUri(id: Long) = Uri.parse("content://com.android.contacts/contacts/$id")

    @Test
    fun view_contact_opens_the_card() {
        // Система передаёт контакт как content-URI: MIME-типа в intent нет.
        val intent = Intent(Intent.ACTION_VIEW, contactUri(42))

        assertEquals(StartAction.OpenContact(42), startActionOf(intent, resolver))
    }

    @Test
    fun edit_contact_opens_the_editor() {
        val intent = Intent(Intent.ACTION_EDIT, contactUri(42))

        assertEquals(StartAction.EditContact(42), startActionOf(intent, resolver))
    }

    @Test
    fun insert_opens_a_new_contact() {
        val intent =
            Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
            }

        assertEquals(StartAction.NewContact, startActionOf(intent, resolver))
    }

    @Test
    fun pick_switches_to_selection_mode() {
        val intent =
            Intent(Intent.ACTION_PICK).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
            }

        assertEquals(StartAction.PickContact, startActionOf(intent, resolver))
    }

    @Test
    fun shared_vcard_is_imported() {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_TEXT, "BEGIN:VCARD\nFN:Anna\nEND:VCARD")
            }

        val action = startActionOf(intent, resolver)
        assertTrue(action is StartAction.ImportVCard)
        assertTrue((action as StartAction.ImportVCard).text.contains("FN:Anna"))
    }

    @Test
    fun app_is_offered_for_contact_and_vcard_intents() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageManager = context.packageManager
        val view =
            Intent(Intent.ACTION_VIEW).apply {
                type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                data = contactUri(1)
            }
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
            }

        assertTrue(
            packageManager
                .queryIntentActivities(view, 0)
                .any { it.activityInfo.packageName == context.packageName },
        )
        assertTrue(
            packageManager
                .queryIntentActivities(send, 0)
                .any { it.activityInfo.packageName == context.packageName },
        )
    }

    @Test
    fun shortcut_actions_are_recognized() {
        val newContact = Intent(ACTION_NEW_CONTACT)
        val favorites = Intent(ACTION_SHOW_FAVORITES)

        assertEquals(StartAction.NewContact, startActionOf(newContact, resolver))
        assertEquals(StartAction.ShowFavorites, startActionOf(favorites, resolver))
    }

    @Test
    fun unknown_intent_opens_the_list() {
        assertEquals(StartAction.List, startActionOf(Intent(Intent.ACTION_MAIN), resolver))
        assertEquals(StartAction.List, startActionOf(null, resolver))
    }
}
