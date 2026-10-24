package com.m4isper.contactsapp.data.datasource.write.contacts

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.provider.ContactsContract
import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.data.datasource.read.ExistingBirthday
import com.m4isper.contactsapp.data.datasource.read.ExistingNickname
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.EventKind
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsExtraWriterTest {

    @Test
    fun relationValues_otherUsesCustomLabel() {
        val relation = EditableRelation(name = "Друг", kind = RelationKind.OTHER, customLabel = "Друг")
        val cv = relationValues(relation)
        assertEquals(
            ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM,
            cv.getAsInteger(ContactsContract.CommonDataKinds.Relation.TYPE),
        )
        assertEquals("Друг", cv.getAsString(ContactsContract.CommonDataKinds.Relation.LABEL))
    }

    @Test
    fun relationOperations_emptyWhenNoItems() {
        val rows = ContactRows()
        val form = ContactForm()
        val ops = relationOperations(1L, rows, form)
        assertTrue(ops.isEmpty())
    }

    @Test
    fun relationOperations_insertsNewItem() {
        val relation = EditableRelation(name = "Мама", kind = RelationKind.MOTHER)
        val form = ContactForm(relations = listOf(relation))
        val rows = ContactRows()
        val ops = relationOperations(1L, rows, form)
        assertEquals(1, ops.size)
    }

    @Test
    fun eventTypeInt_mapsEventKind() {
        assertEquals(android.provider.ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,
            eventTypeInt(EditableEvent(kind = EventKind.ANNIVERSARY)))
        assertEquals(android.provider.ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM,
            eventTypeInt(EditableEvent(kind = EventKind.OTHER, customLabel = "День рождения")))
        assertEquals(android.provider.ContactsContract.CommonDataKinds.Event.TYPE_OTHER,
            eventTypeInt(EditableEvent(kind = EventKind.OTHER)))
    }

    @Test
    fun preservedEventValues_keepsRawDate() {
        val event = EditableEvent(kind = EventKind.OTHER, customLabel = "День")
        val cv = preservedEventValues("2020-01-01", event)
        assertEquals("2020-01-01", cv.getAsString(ContactsContract.CommonDataKinds.Event.START_DATE))
        assertEquals(
            ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM,
            cv.getAsInteger(ContactsContract.CommonDataKinds.Event.TYPE),
        )
        assertEquals("День", cv.getAsString(ContactsContract.CommonDataKinds.Event.LABEL))
    }

    @Test
    fun eventOperations_emptyWhenNoItems() {
        val rows = ContactRows()
        val form = ContactForm()
        val ops = eventOperations(1L, rows, form)
        assertTrue(ops.isEmpty())
    }

    @Test
    fun birthdayWriteOperations_noneWhenNoExistingAndNoValue() {
        val ops = birthdayWriteOperations(1L, null, null)
        assertTrue(ops.isEmpty())
    }

    @Test
    fun birthdayWriteOperations_insertWhenNewValue() {
        val existing: ExistingBirthday? = null
        val value = Birthday(year = 1990, month = 5, day = 15)
        val ops = birthdayWriteOperations(1L, existing, value)
        assertEquals(1, ops.size)
    }

    @Test
    fun birthdayWriteOperations_deletesWhenExistingAndNoValue() {
        val existing = ExistingBirthday(dataId = 1L, rawContactId = 1L, raw = "1990-05-15")
        val ops = birthdayWriteOperations(1L, existing, null)
        assertEquals(1, ops.size)
    }

    @Test
    fun nicknameValues_setsNameAndType() {
        val cv = nicknameValues("Johnny")
        assertEquals("Johnny", cv.getAsString(ContactsContract.CommonDataKinds.Nickname.NAME))
        assertEquals(
            ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT,
            cv.getAsInteger(ContactsContract.CommonDataKinds.Nickname.TYPE),
        )
    }

    @Test
    fun nicknameOperations_insertsWhenMissing() {
        val ops = nicknameOperations(1L, null, "Johnny")
        assertEquals(1, ops.size)
    }

    @Test
    fun nicknameOperations_deletesWhenBlank() {
        val existing = ExistingNickname(dataId = 1L, rawContactId = 1L, nickname = "Johnny")
        val ops = nicknameOperations(1L, existing, "")
        assertEquals(1, ops.size)
    }

    @Test
    fun nicknameOperations_emptyWhenUnchanged() {
        val existing = ExistingNickname(dataId = 1L, rawContactId = 1L, nickname = "Johnny")
        val ops = nicknameOperations(1L, existing, "Johnny")
        assertTrue(ops.isEmpty())
    }

    @Test
    fun nicknameOperations_updatesWhenChanged() {
        val existing = ExistingNickname(dataId = 1L, rawContactId = 1L, nickname = "Johnny")
        val ops = nicknameOperations(1L, existing, "Jonny")
        assertEquals(1, ops.size)
    }
}
