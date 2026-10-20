package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactFormNormalizationTest {
    @Test
    fun `blank rows are dropped`() {
        val form =
            ContactForm(
                phones = listOf(EditablePhone(number = "111"), EditablePhone(number = "  ")),
                emails = listOf(EditableEmail(address = " ")),
                addresses = listOf(EditableAddress(address = " ")),
                websites = listOf(EditableWebsite(url = "")),
                instantMessages = listOf(EditableInstantMessage(address = " ")),
                relations = listOf(EditableRelation(name = " ")),
                events = listOf(EditableEvent(date = null)),
            )

        val normalized = form.normalized()

        assertEquals(listOf(EditablePhone(number = "111")), normalized.phones)
        assertEquals(emptyList<EditableEmail>(), normalized.emails)
        assertEquals(emptyList<EditableAddress>(), normalized.addresses)
        assertEquals(emptyList<EditableWebsite>(), normalized.websites)
        assertEquals(emptyList<EditableInstantMessage>(), normalized.instantMessages)
        assertEquals(emptyList<EditableRelation>(), normalized.relations)
        assertEquals(emptyList<EditableEvent>(), normalized.events)
    }

    @Test
    fun `blank custom labels and protocols become null`() {
        val normalized =
            ContactForm(
                phones = listOf(EditablePhone(number = "111", customLabel = "  ")),
                emails = listOf(EditableEmail(address = "a@x.com", customLabel = "")),
                addresses = listOf(EditableAddress(address = "Street", customLabel = " ")),
                instantMessages =
                    listOf(
                        EditableInstantMessage(
                            address = "anna",
                            protocol = ImProtocol.CUSTOM,
                            customProtocol = " ",
                        ),
                    ),
                relations = listOf(EditableRelation(name = "Bob", customLabel = " ")),
                events = listOf(EditableEvent(date = Birthday(day = 1, month = 6), customLabel = "  ")),
            ).normalized()

        assertEquals(null, normalized.phones.single().customLabel)
        assertEquals(null, normalized.emails.single().customLabel)
        assertEquals(null, normalized.addresses.single().customLabel)
        assertEquals(null, normalized.instantMessages.single().customProtocol)
        assertEquals(null, normalized.relations.single().customLabel)
        assertEquals(null, normalized.events.single().customLabel)
    }

    @Test
    fun `filled values are kept and groups are sorted`() {
        val form =
            ContactForm(
                name = EditableName(display = "Anna"),
                phones = listOf(EditablePhone(rowId = 3, number = "111", customLabel = "работа")),
                events = listOf(EditableEvent(unparsedDate = "1985-03-0X")),
                groups = listOf(5L, 2L),
            )

        val normalized = form.normalized()

        assertEquals(form.name, normalized.name)
        assertEquals(listOf(3L), normalized.phones.map { it.rowId })
        assertEquals("работа", normalized.phones.single().customLabel)
        assertEquals("1985-03-0X", normalized.events.single().unparsedDate)
        assertEquals(listOf(2L, 5L), normalized.groups)
    }
}
