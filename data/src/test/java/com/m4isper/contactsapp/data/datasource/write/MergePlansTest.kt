package com.m4isper.contactsapp.data.datasource.write

import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditableNickname
import com.m4isper.contactsapp.domain.model.EditableNote
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.model.EditablePhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MergePlansTest {
    @Test
    fun `target values keep their data ids and go first`() {
        val target = ContactForm(phones = listOf(EditablePhone(rowId = 1, number = "+7111")))
        val source = ContactForm(phones = listOf(EditablePhone(rowId = 2, number = "+7222")))

        val merged = mergeContactForms(target, listOf(source))

        assertEquals(listOf(1L, null), merged.phones.map { it.rowId })
        assertEquals(listOf("+7111", "+7222"), merged.phones.map { it.number })
    }

    @Test
    fun `duplicate values are not added twice`() {
        val target = ContactForm(phones = listOf(EditablePhone(rowId = 1, number = "+7 (111) 222")))
        val source = ContactForm(phones = listOf(EditablePhone(rowId = 2, number = "+7111222")))

        val merged = mergeContactForms(target, listOf(source))

        assertEquals(listOf("+7 (111) 222"), merged.phones.map { it.number })
    }

    @Test
    fun `blank values of the source are skipped`() {
        val source =
            ContactForm(
                phones = listOf(EditablePhone(number = "   ")),
                emails = listOf(EditableEmail(address = "")),
            )

        val merged = mergeContactForms(ContactForm(), listOf(source))

        assertTrue(merged.phones.isEmpty())
        assertTrue(merged.emails.isEmpty())
    }

    @Test
    fun `name falls back to the first filled source`() {
        val empty = ContactForm()
        val named = ContactForm(name = EditableName(display = "Anna"))
        val target = ContactForm(name = EditableName(display = "Bob"))

        assertEquals("Anna", mergeContactForms(empty, listOf(empty, named)).name.display)
        assertEquals("Bob", mergeContactForms(target, listOf(named)).name.display)
    }

    @Test
    fun `single fields fall back to the first filled source`() {
        val empty = ContactForm()
        val filled =
            ContactForm(
                organization = EditableOrganization(rowId = 5, company = "ACME", title = "CTO"),
                note = EditableNote(rowId = 6, note = "важно"),
                birthday = EditableBirthday(rowId = 7, value = Birthday(day = 1, month = 6)),
                nickname = EditableNickname(rowId = 8, nickname = "Нюша"),
            )

        val merged = mergeContactForms(empty, listOf(empty, filled))

        assertEquals("ACME", merged.organization.company)
        assertNull(merged.organization.rowId)
        assertEquals("важно", merged.note.note)
        assertEquals(Birthday(day = 1, month = 6), merged.birthday.value)
        assertEquals("Нюша", merged.nickname.nickname)
    }

    @Test
    fun `target single fields win over sources`() {
        val target = ContactForm(organization = EditableOrganization(rowId = 1, company = "ACME"))
        val source = ContactForm(organization = EditableOrganization(rowId = 2, company = "Другая"))

        val merged = mergeContactForms(target, listOf(source))

        assertEquals(1L, merged.organization.rowId)
        assertEquals("ACME", merged.organization.company)
    }

    @Test
    fun `groups are united without duplicates`() {
        val target = ContactForm(groups = listOf(1, 2))
        val first = ContactForm(groups = listOf(2, 3))
        val second = ContactForm(groups = listOf(4))

        val merged = mergeContactForms(target, listOf(first, second))

        assertEquals(listOf(1L, 2L, 3L, 4L), merged.groups)
    }

    @Test
    fun `target photo is kept`() {
        val target = ContactForm(photoUri = "target")
        val source = ContactForm(photoUri = "source")

        val merged = mergeContactForms(target, listOf(source))

        assertEquals("target", merged.photoUri)
        assertEquals(false, merged.photoChanged)
    }

    @Test
    fun `source photo is adopted when the target has none`() {
        val merged = mergeContactForms(ContactForm(), listOf(ContactForm(photoUri = "source")))

        assertEquals("source", merged.photoUri)
        assertEquals(true, merged.photoChanged)
    }

    @Test
    fun `source events without a parsed date are not merged`() {
        val source =
            ContactForm(
                events =
                    listOf(
                        EditableEvent(rowId = 2, unparsedDate = "когда-то"),
                        EditableEvent(rowId = 3, date = Birthday(day = 1, month = 1)),
                    ),
            )

        val merged = mergeContactForms(ContactForm(), listOf(source))

        assertEquals(1, merged.events.size)
        assertEquals(Birthday(day = 1, month = 1), merged.events.single().date)
        assertNull(merged.events.single().rowId)
    }

    @Test
    fun `event with the same date and kind is not duplicated`() {
        val target = ContactForm(events = listOf(EditableEvent(rowId = 1, date = Birthday(day = 6, month = 5))))
        val source = ContactForm(events = listOf(EditableEvent(rowId = 2, date = Birthday(day = 6, month = 5))))

        val merged = mergeContactForms(target, listOf(source))

        assertEquals(1, merged.events.size)
        assertEquals(1L, merged.events.single().rowId)
    }
}
