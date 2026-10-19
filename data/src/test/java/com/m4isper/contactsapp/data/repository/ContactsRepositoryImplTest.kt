package com.m4isper.contactsapp.data.repository

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import com.m4isper.contactsapp.data.datasource.ContactsDataSource
import com.m4isper.contactsapp.data.datasource.ContactsWriterDataSource
import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.data.datasource.read.ExistingBirthday
import com.m4isper.contactsapp.data.datasource.read.ExistingEmail
import com.m4isper.contactsapp.data.datasource.read.ExistingEvent
import com.m4isper.contactsapp.data.datasource.read.ExistingGroupMembership
import com.m4isper.contactsapp.data.datasource.read.ExistingInstantMessage
import com.m4isper.contactsapp.data.datasource.read.ExistingName
import com.m4isper.contactsapp.data.datasource.read.ExistingNickname
import com.m4isper.contactsapp.data.datasource.read.ExistingNote
import com.m4isper.contactsapp.data.datasource.read.ExistingOrganization
import com.m4isper.contactsapp.data.datasource.read.ExistingPhone
import com.m4isper.contactsapp.data.datasource.read.ExistingRelation
import com.m4isper.contactsapp.data.datasource.read.ExistingWebsite
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.RelationKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsRepositoryImplTest {
    private val details = ContactDetails(contactId = 7, name = "Anna")

    @Test
    fun `form is null for an unknown contact`() {
        val repository = ContactsRepositoryImpl(FakeReader(details = null), FakeWriter())

        assertNull(runBlocking { repository.getContactForm(7) })
    }

    @Test
    fun `form maps rows into editable fields`() {
        val rows =
            ContactRows(
                names =
                    listOf(
                        name(
                            dataId = 1,
                            rawContactId = 10,
                            display = "Anna Petrova",
                            given = "Anna",
                            family = "Petrova",
                        ),
                    ),
                phones = listOf(ExistingPhone(2, 10, "+71112223344", Phone.TYPE_MOBILE, null)),
                emails = listOf(ExistingEmail(3, 10, "a@x.com", Email.TYPE_CUSTOM, "Рабочая")),
                websites = listOf(ExistingWebsite(4, 10, "https://x.dev")),
                instantMessages =
                    listOf(
                        ExistingInstantMessage(
                            dataId = 5,
                            rawContactId = 10,
                            address = "skype:max",
                            type = Im.TYPE_OTHER,
                            protocol = Im.PROTOCOL_SKYPE,
                            customProtocol = null,
                        ),
                    ),
                events = listOf(ExistingEvent(6, 10, "2020-09-10", Event.TYPE_CUSTOM, "Свадьба")),
                birthday = ExistingBirthday(7, 10, "--05-06"),
                nickname = ExistingNickname(8, 10, "Нюша"),
                note = ExistingNote(9, 10, "заметка"),
                organization = ExistingOrganization(10, 10, "ACME", "CTO"),
                relations = listOf(ExistingRelation(11, 10, "Bob", Relation.TYPE_FRIEND, null)),
                groupMemberships =
                    listOf(
                        ExistingGroupMembership(dataId = 12, rawContactId = 10, groupId = 100),
                        ExistingGroupMembership(dataId = 13, rawContactId = 10, groupId = 200),
                    ),
            )
        val repository = ContactsRepositoryImpl(FakeReader(details), FakeWriter(rows = rows))

        val form = requireNotNull(runBlocking { repository.getContactForm(7) })

        assertEquals(NameMode.STRUCTURED, form.name.mode)
        assertEquals("Anna Petrova", form.name.display)
        assertEquals("Anna", form.name.given)
        assertEquals(PhoneKind.MOBILE, form.phones.single().kind)
        assertEquals("+71112223344", form.phones.single().number)
        assertEquals(EmailKind.OTHER, form.emails.single().kind)
        assertEquals("Рабочая", form.emails.single().customLabel)
        assertEquals("https://x.dev", form.websites.single().url)
        assertEquals(ImProtocol.SKYPE, form.instantMessages.single().protocol)
        assertNull(form.instantMessages.single().customProtocol)
        assertEquals(EventKind.OTHER, form.events.single().kind)
        assertEquals("Свадьба", form.events.single().customLabel)
        assertEquals(Birthday(day = 6, month = 5, year = null), form.birthday.value)
        assertEquals("Нюша", form.nickname.nickname)
        assertEquals("заметка", form.note.note)
        assertEquals("ACME", form.organization.company)
        assertEquals("CTO", form.organization.title)
        assertEquals(RelationKind.FRIEND, form.relations.single().kind)
        assertEquals(listOf(100L, 200L), form.groups)
        assertEquals(false, form.photoChanged)
    }

    @Test
    fun `form keeps an unparsed event date`() {
        val rows =
            ContactRows(
                events = listOf(ExistingEvent(1, 10, "когда-то", Event.TYPE_CUSTOM, null)),
            )
        val repository = ContactsRepositoryImpl(FakeReader(details), FakeWriter(rows = rows))

        val event = requireNotNull(runBlocking { repository.getContactForm(7) }).events.single()

        assertNull(event.date)
        assertEquals("когда-то", event.unparsedDate)
    }

    @Test
    fun `form prefers the name of the target raw contact`() {
        val rows =
            ContactRows(
                names =
                    listOf(
                        name(dataId = 1, rawContactId = 10, given = "Старое"),
                        name(dataId = 2, rawContactId = 20, given = "Новое"),
                    ),
            )
        val repository = ContactsRepositoryImpl(FakeReader(details), FakeWriter(rawContactId = 20, rows = rows))

        val form = requireNotNull(runBlocking { repository.getContactForm(7) })

        assertEquals("Новое", form.name.given)
    }

    @Test
    fun `form falls back to the aggregated name in display mode`() {
        val repository = ContactsRepositoryImpl(FakeReader(details), FakeWriter())

        val form = requireNotNull(runBlocking { repository.getContactForm(7) })

        assertEquals(NameMode.DISPLAY, form.name.mode)
        assertEquals("Anna", form.name.display)
    }

    @Test
    fun `update sends rows and the form to the writer`() {
        val rows = ContactRows(phones = listOf(ExistingPhone(2, 10, "+7", Phone.TYPE_MOBILE, null)))
        val writer = FakeWriter(rawContactId = 10, rows = rows)
        val repository = ContactsRepositoryImpl(FakeReader(details), writer)
        val form = ContactForm(name = EditableName(display = "Anna"))

        runBlocking { repository.updateContact(7, form) }

        val update = writer.updated.single()
        assertEquals(10L, update.rawContactId)
        assertEquals(rows, update.rows)
        assertEquals(form, update.form)
    }

    @Test
    fun `update fails when the contact has no raw row`() {
        val repository = ContactsRepositoryImpl(FakeReader(details), FakeWriter(rawContactId = null))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateContact(7, ContactForm()) }
        }
    }

    @Test
    fun `create and delete go to the writer`() {
        val writer = FakeWriter()
        val repository = ContactsRepositoryImpl(FakeReader(details), writer)
        val form = ContactForm()

        runBlocking {
            repository.createContact(form)
            repository.deleteContact(7)
        }

        assertEquals(listOf(form), writer.created)
        assertEquals(listOf(7L), writer.deleted)
    }

    @Test
    fun `merge moves source data into the target and deletes sources`() {
        val target =
            ContactDetails(
                contactId = 7,
                name = "Anna",
                phones = listOf(PhoneNumber("+7111", PhoneKind.MOBILE)),
            )
        val source =
            ContactDetails(
                contactId = 8,
                name = "Anna",
                emails = listOf(EmailAddress("a@x.com", EmailKind.HOME)),
            )
        val targetRows = ContactRows(phones = listOf(ExistingPhone(1, 10, "+7111", Phone.TYPE_MOBILE, null)))
        val sourceRows = ContactRows(emails = listOf(ExistingEmail(2, 20, "a@x.com", Email.TYPE_HOME, null)))
        val writer =
            FakeWriter(
                rawContactIdsByContact = mapOf(7L to 10L, 8L to 20L),
                rowsByContact = mapOf(7L to targetRows, 8L to sourceRows),
            )
        val repository = ContactsRepositoryImpl(FakeReader(byId = mapOf(7L to target, 8L to source)), writer)

        runBlocking { repository.mergeContacts(targetId = 7, sourceIds = listOf(8)) }

        val update = writer.updated.single()
        assertEquals(10L, update.rawContactId)
        assertEquals(targetRows, update.rows)
        assertEquals(listOf("+7111"), update.form.phones.map { it.number })
        assertEquals(listOf("a@x.com"), update.form.emails.map { it.address })
        assertNull(
            update
                .form
                .emails
                .single()
                .dataId,
        )
        assertEquals(listOf(8L), writer.deleted)
    }

    @Test
    fun `merge ignores sources that no longer exist`() {
        val writer = FakeWriter()
        val repository = ContactsRepositoryImpl(FakeReader(details), writer)

        runBlocking { repository.mergeContacts(targetId = 7, sourceIds = listOf(99)) }

        assertTrue(writer.updated.isEmpty())
        assertTrue(writer.deleted.isEmpty())
    }

    @Test
    fun `merge fails when the target is missing`() {
        val writer = FakeWriter()
        val source = ContactDetails(contactId = 8, name = "Bob")
        val repository = ContactsRepositoryImpl(FakeReader(byId = mapOf(8L to source)), writer)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.mergeContacts(targetId = 7, sourceIds = listOf(8)) }
        }
        assertTrue(writer.deleted.isEmpty())
    }

    private fun name(
        dataId: Long,
        rawContactId: Long,
        display: String = "",
        given: String = "",
        family: String = "",
    ) = ExistingName(
        dataId = dataId,
        rawContactId = rawContactId,
        display = display,
        given = given,
        family = family,
        middle = "",
        prefix = "",
        suffix = "",
    )
}

private data class UpdateCall(
    val rawContactId: Long,
    val rows: ContactRows,
    val form: ContactForm,
)

private class FakeReader(
    private val details: ContactDetails? = null,
    private val byId: Map<Long, ContactDetails> = emptyMap(),
) : ContactsDataSource {
    override suspend fun loadContacts(): List<ContactDetails> = listOfNotNull(details) + byId.values

    override suspend fun loadContact(contactId: Long): ContactDetails? =
        byId[contactId] ?: details?.takeIf { it.contactId == contactId }
}

private class FakeWriter(
    private val rawContactId: Long? = 10,
    private val rows: ContactRows = ContactRows(),
    private val rawContactIdsByContact: Map<Long, Long> = emptyMap(),
    private val rowsByContact: Map<Long, ContactRows> = emptyMap(),
) : ContactsWriterDataSource {
    val created = mutableListOf<ContactForm>()
    val updated = mutableListOf<UpdateCall>()
    val deleted = mutableListOf<Long>()

    override suspend fun createContact(form: ContactForm) {
        created += form
    }

    override suspend fun updateContact(
        rawContactId: Long,
        rows: ContactRows,
        form: ContactForm,
    ) {
        updated += UpdateCall(rawContactId = rawContactId, rows = rows, form = form)
    }

    override suspend fun loadContactRows(contactId: Long): ContactRows = rowsByContact[contactId] ?: rows

    override suspend fun loadRawContactId(contactId: Long): Long? = rawContactIdsByContact[contactId] ?: rawContactId

    override suspend fun deleteContact(contactId: Long) {
        deleted += contactId
    }
}
