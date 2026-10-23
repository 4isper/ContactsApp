package com.m4isper.contactsapp.domain.duplicates

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.vcard.VCardContact
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportDuplicatesTest {
    @Test
    fun findImportDuplicates_matchesByPhone() {
        val contact = VCardContact(form = ContactForm(phones = listOf(EditablePhone(number = "1234567"))))
        val existing =
            ContactDetails(
                contactId = 1L,
                name = "X",
                phones =
                    listOf(
                        com.m4isper.contactsapp.domain.model.contact.PhoneNumber(
                            "1234567",
                            com.m4isper.contactsapp.domain.model.contact.PhoneKind.MOBILE,
                        ),
                    ),
            )
        val result = findImportDuplicates(contact, listOf(existing))
        assertEquals(listOf(1L), result.map { it.contactId })
    }

    @Test
    fun findImportDuplicates_matchesByEmail() {
        val contact = VCardContact(form = ContactForm(emails = listOf(EditableEmail(address = "a@b.c"))))
        val existing =
            ContactDetails(
                contactId = 2L,
                name = "Y",
                emails =
                    listOf(
                        com.m4isper.contactsapp.domain.model.contact.EmailAddress(
                            "A@B.C",
                            com.m4isper.contactsapp.domain.model.contact.EmailKind.HOME,
                        ),
                    ),
            )
        val result = findImportDuplicates(contact, listOf(existing))
        assertEquals(listOf(2L), result.map { it.contactId })
    }

    @Test
    fun findImportDuplicates_returnsEmptyWhenNoMatch() {
        val contact = VCardContact(form = ContactForm(phones = listOf(EditablePhone(number = "9999999"))))
        val existing =
            ContactDetails(
                contactId = 3L,
                name = "Z",
                phones =
                    listOf(
                        com.m4isper.contactsapp.domain.model.contact.PhoneNumber(
                            "1111111",
                            com.m4isper.contactsapp.domain.model.contact.PhoneKind.MOBILE,
                        ),
                    ),
            )
        assertTrue(findImportDuplicates(contact, listOf(existing)).isEmpty())
    }

    @Test
    fun findImportDuplicates_ignoresShortPhones() {
        val contact = VCardContact(form = ContactForm(phones = listOf(EditablePhone(number = "123"))))
        val existing =
            ContactDetails(
                contactId = 4L,
                name = "W",
                phones =
                    listOf(
                        com.m4isper.contactsapp.domain.model.contact.PhoneNumber(
                            "123",
                            com.m4isper.contactsapp.domain.model.contact.PhoneKind.MOBILE,
                        ),
                    ),
            )
        assertTrue(findImportDuplicates(contact, listOf(existing)).isEmpty())
    }
}
