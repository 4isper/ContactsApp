package com.m4isper.contactsapp.domain.duplicates

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

private fun contact(
    id: Long,
    name: String = "Anna Petrova",
    phones: List<String> = emptyList(),
    emails: List<String> = emptyList(),
) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
    emails = emails.map { EmailAddress(it, EmailKind.HOME) },
)

class DuplicateDetectionTest {
    @Test
    fun `contacts sharing a phone are grouped`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1, phones = listOf("+7 111 222-33-44")),
                    contact(2, phones = listOf("71112223344")),
                ),
            )

        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().members.map { it.contactId })
        assertFalse(groups.single().nameOnly)
    }

    @Test
    fun `email match is case insensitive`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1, emails = listOf("A@X.com")),
                    contact(2, emails = listOf(" a@x.com ")),
                ),
            )

        assertEquals(1, groups.size)
        assertFalse(groups.single().nameOnly)
    }

    @Test
    fun `email matching does not depend on the default locale`() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val groups =
                findDuplicateGroups(
                    listOf(
                        contact(1, emails = listOf("INFO@X.com")),
                        contact(2, emails = listOf("info@x.com")),
                    ),
                )

            assertEquals(1, groups.size)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun `matches are transitive`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1, phones = listOf("+7111222")),
                    contact(2, phones = listOf("+7111222", "+7333444")),
                    contact(3, phones = listOf("+7333444")),
                ),
            )

        assertEquals(1, groups.size)
        assertEquals(listOf(2L, 1L, 3L), groups.single().members.map { it.contactId })
    }

    @Test
    fun `short phone numbers do not count`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1, name = "Anna", phones = listOf("111")),
                    contact(2, name = "Bob", phones = listOf("111")),
                ),
            )

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `full name match yields a name-only group`() {
        val groups = findDuplicateGroups(listOf(contact(1), contact(2)))

        assertEquals(1, groups.size)
        assertTrue(groups.single().nameOnly)
    }

    @Test
    fun `single-word names are not matched`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1, name = "Alexander"),
                    contact(2, name = "Alexander"),
                ),
            )

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `shared phone wins over name-only flag`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1, phones = listOf("+7111222")),
                    contact(2, phones = listOf("+7111222")),
                ),
            )

        assertEquals(1, groups.size)
        assertFalse(groups.single().nameOnly)
    }

    @Test
    fun `the most complete contact goes first`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1),
                    contact(2, phones = listOf("+7111222"), emails = listOf("a@x.com")),
                ),
            )

        assertEquals(listOf(2L, 1L), groups.single().members.map { it.contactId })
    }

    @Test
    fun `different contacts produce no groups`() {
        val groups =
            findDuplicateGroups(
                listOf(
                    contact(1, name = "Anna", phones = listOf("+7111222")),
                    contact(2, name = "Bob", phones = listOf("+7333444")),
                ),
            )

        assertTrue(groups.isEmpty())
    }
}
