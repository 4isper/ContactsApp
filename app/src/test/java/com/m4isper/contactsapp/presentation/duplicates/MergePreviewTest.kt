package com.m4isper.contactsapp.presentation.duplicates

import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergePreviewTest {
    private val target =
        ContactDetails(
            contactId = 1,
            name = "Anna",
            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
            emails = listOf(EmailAddress("anna@example.com", EmailKind.HOME)),
        )

    @Test
    fun `lists only the fields the target does not have`() {
        val source =
            ContactDetails(
                contactId = 2,
                name = "Anna",
                phones = listOf(PhoneNumber("111", PhoneKind.MOBILE), PhoneNumber("222", PhoneKind.WORK)),
                emails = listOf(EmailAddress("anna@example.com", EmailKind.HOME)),
                note = "Любит кофе",
            )

        val additions = mergeAdditions(target, listOf(source))

        assertEquals(
            listOf(
                MergeAddition(MergeField.PHONE, "222"),
                MergeAddition(MergeField.NOTE, "Любит кофе"),
            ),
            additions,
        )
    }

    @Test
    fun `different name is a new field`() {
        val source = ContactDetails(contactId = 2, name = "Anna Ivanova")

        val additions = mergeAdditions(target, listOf(source))

        assertEquals(listOf(MergeAddition(MergeField.NAME, "Anna Ivanova")), additions)
    }

    @Test
    fun `birthday and organization are shown`() {
        val source =
            ContactDetails(
                contactId = 2,
                name = "Anna",
                birthday = Birthday(day = 1, month = 6, year = 1990),
                organization =
                    com.m4isper.contactsapp.domain.model
                        .Organization("ACME", "CTO"),
            )

        val additions = mergeAdditions(target, listOf(source))

        assertEquals(2, additions.size)
        assertTrue(additions.any { it.field == MergeField.BIRTHDAY })
        assertTrue(additions.any { it.field == MergeField.ORGANIZATION })
    }

    @Test
    fun `identical contacts have nothing to add`() {
        val additions = mergeAdditions(target, listOf(target.copy(contactId = 2)))

        assertTrue(additions.isEmpty())
    }
}
