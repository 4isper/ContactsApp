package com.m4isper.contactsapp.domain.model

import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.EditableSipAddress
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.domain.model.contact.ImProtocol
import com.m4isper.contactsapp.domain.model.contact.normalized
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
    fun `at most one phone and one email stay primary`() {
        val normalized =
            ContactForm(
                phones =
                    listOf(
                        EditablePhone(number = "111", isPrimary = true),
                        EditablePhone(number = "222", isPrimary = true),
                    ),
                emails =
                    listOf(
                        EditableEmail(address = "a@x.com", isPrimary = true),
                        EditableEmail(address = "b@x.com", isPrimary = true),
                    ),
            ).normalized()

        assertEquals(listOf(true, false), normalized.phones.map { it.isPrimary })
        assertEquals(listOf(true, false), normalized.emails.map { it.isPrimary })
    }

    @Test
    fun `blank sip addresses are dropped`() {
        val normalized =
            ContactForm(
                sipAddresses =
                    listOf(
                        EditableSipAddress(address = "sip:anna@example.com"),
                        EditableSipAddress(address = "  "),
                    ),
            ).normalized()

        assertEquals(listOf("sip:anna@example.com"), normalized.sipAddresses.map { it.address })
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
