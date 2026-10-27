package com.m4isper.contactsapp.data.datasource.write.contacts

import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsListOperationsFullTest {
    @Test
    fun phoneListOperations_emptyWhenNoItems() {
        assertTrue(
            phoneListOperations(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                com.m4isper.contactsapp.domain.model.contact
                    .ContactForm(),
            ).isEmpty(),
        )
    }

    @Test
    fun phoneListOperations_insertsNewPhone() {
        val form =
            com.m4isper.contactsapp.domain.model.contact.ContactForm(
                phones = listOf(EditablePhone(number = "123")),
            )
        val ops =
            phoneListOperations(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                form,
            )
        assertEquals(1, ops.size)
    }

    @Test
    fun emailListOperations_emptyWhenNoItems() {
        assertTrue(
            emailListOperations(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                com.m4isper.contactsapp.domain.model.contact
                    .ContactForm(),
            ).isEmpty(),
        )
    }

    @Test
    fun addressListOperations_emptyWhenNoItems() {
        assertTrue(
            addressListOperations(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                com.m4isper.contactsapp.domain.model.contact
                    .ContactForm(),
            ).isEmpty(),
        )
    }

    @Test
    fun websiteListOperations_emptyWhenNoItems() {
        assertTrue(
            websiteListOperations(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                com.m4isper.contactsapp.domain.model.contact
                    .ContactForm(),
            ).isEmpty(),
        )
    }

    @Test
    fun sipListOperations_emptyWhenNoItems() {
        assertTrue(
            sipListOperations(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                com.m4isper.contactsapp.domain.model.contact
                    .ContactForm(),
            ).isEmpty(),
        )
    }
}
