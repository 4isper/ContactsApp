package com.m4isper.contactsapp.presentation.duplicates

import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.Organization
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
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

    /** Тестовый форматтер: в UI передают birthdayText, здесь — читаемый дамп. */
    private fun additions(
        target: ContactDetails,
        sources: List<ContactDetails>,
    ): List<MergeAddition> =
        mergeAdditions(
            target,
            sources,
            formatBirthday = { birthday -> "${birthday.day}.${birthday.month}.${birthday.year ?: "--"}" },
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

        val additions = additions(target, listOf(source))

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

        val additions = additions(target, listOf(source))

        assertEquals(listOf(MergeAddition(MergeField.NAME, "Anna Ivanova")), additions)
    }

    @Test
    fun `birthday is formatted by the provided formatter`() {
        val source =
            ContactDetails(
                contactId = 2,
                name = "Anna",
                birthday = Birthday(day = 1, month = 6, year = 1990),
                organization =
                    com.m4isper.contactsapp.domain.model.contact
                        .Organization("ACME", "CTO"),
            )

        val additions = additions(target, listOf(source))

        assertEquals(2, additions.size)
        assertEquals("1.6.1990", additions.first { it.field == MergeField.BIRTHDAY }.value)
        assertTrue(additions.any { it.field == MergeField.ORGANIZATION })
    }

    @Test
    fun `identical contacts have nothing to add`() {
        val additions = additions(target, listOf(target.copy(contactId = 2)))

        assertTrue(additions.isEmpty())
    }
}
