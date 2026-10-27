package com.m4isper.contactsapp.data.datasource.write.contacts

import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.data.datasource.read.ExistingPhone
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsListOperationsTest {
    @Test
    fun phoneListOperations_emptyWhenNoItems() {
        assertTrue(phoneListOperations(1L, ContactRows(), ContactForm()).isEmpty())
    }

    @Test
    fun phoneListOperations_insertsNewPhone() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "123")))
        val ops = phoneListOperations(1L, ContactRows(), form)
        assertEquals(1, ops.size)
    }

    @Test
    fun phoneListOperations_updatesExistingPhone() {
        val existing = ExistingPhone(dataId = 10L, rawContactId = 1L, number = "old", type = 1, label = null)
        val rows = ContactRows(phones = listOf(existing))
        val form = ContactForm(phones = listOf(EditablePhone(rowId = 10L, number = "new")))
        val ops = phoneListOperations(1L, rows, form)
        assertEquals(1, ops.size)
    }

    @Test
    fun phoneListOperations_deletesRemovedPhone() {
        val existing = ExistingPhone(dataId = 10L, rawContactId = 1L, number = "old", type = 1, label = null)
        val rows = ContactRows(phones = listOf(existing))
        val ops = phoneListOperations(1L, rows, ContactForm())
        assertEquals(1, ops.size)
    }

    @Test
    fun emailListOperations_emptyWhenNoItems() {
        assertTrue(emailListOperations(1L, ContactRows(), ContactForm()).isEmpty())
    }

    @Test
    fun addressListOperations_emptyWhenNoItems() {
        assertTrue(addressListOperations(1L, ContactRows(), ContactForm()).isEmpty())
    }

    @Test
    fun websiteListOperations_emptyWhenNoItems() {
        assertTrue(websiteListOperations(1L, ContactRows(), ContactForm()).isEmpty())
    }

    @Test
    fun sipListOperations_emptyWhenNoItems() {
        assertTrue(sipListOperations(1L, ContactRows(), ContactForm()).isEmpty())
    }
}
