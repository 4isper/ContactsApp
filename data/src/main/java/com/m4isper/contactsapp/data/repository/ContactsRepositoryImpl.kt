package com.m4isper.contactsapp.data.repository

import android.provider.ContactsContract
import com.m4isper.contactsapp.data.datasource.ContactsDataSource
import com.m4isper.contactsapp.data.datasource.ContactsWriterDataSource
import com.m4isper.contactsapp.data.datasource.ExistingAddress
import com.m4isper.contactsapp.data.datasource.ExistingEmail
import com.m4isper.contactsapp.data.datasource.ExistingEvent
import com.m4isper.contactsapp.data.datasource.ExistingInstantMessage
import com.m4isper.contactsapp.data.datasource.ExistingName
import com.m4isper.contactsapp.data.datasource.ExistingPhone
import com.m4isper.contactsapp.data.datasource.ExistingRelation
import com.m4isper.contactsapp.data.datasource.customLabelOf
import com.m4isper.contactsapp.data.datasource.mapAddressKind
import com.m4isper.contactsapp.data.datasource.mapEmailKind
import com.m4isper.contactsapp.data.datasource.mapEventKind
import com.m4isper.contactsapp.data.datasource.mapImProtocol
import com.m4isper.contactsapp.data.datasource.mapPhoneKind
import com.m4isper.contactsapp.data.datasource.mapRelationKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableAddress
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditableNickname
import com.m4isper.contactsapp.domain.model.EditableNote
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.EditableWebsite
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContactsRepositoryImpl(
    private val dataSource: ContactsDataSource,
    private val writer: ContactsWriterDataSource,
) : ContactsRepository {
    override suspend fun getContacts(): List<ContactDetails> = dataSource.loadContacts()

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = dataSource.loadContact(contactId)

    override suspend fun getContactForm(contactId: Long): ContactForm? {
        val details = dataSource.loadContact(contactId) ?: return null
        val rows = writer.loadContactRows(contactId)
        val rawContactId = writer.loadRawContactId(contactId)
        return ContactForm(
            name = editableNameOf(rows.names.preferredFor(rawContactId), details),
            phones = rows.phones.map { it.toEditable() },
            emails = rows.emails.map { it.toEditable() },
            addresses = rows.addresses.map { it.toEditable() },
            websites = rows.websites.map { EditableWebsite(dataId = it.dataId, url = it.url) },
            instantMessages = rows.instantMessages.map { it.toEditable() },
            organization =
                rows.organization
                    ?.let { EditableOrganization(dataId = it.dataId, company = it.company, title = it.title) }
                    ?: EditableOrganization(),
            note = rows.note?.let { EditableNote(dataId = it.dataId, note = it.note) } ?: EditableNote(),
            birthday =
                rows.birthday
                    ?.let { EditableBirthday(dataId = it.dataId, value = Birthday.parse(it.raw)) }
                    ?: EditableBirthday(),
            nickname =
                rows.nickname
                    ?.let { EditableNickname(dataId = it.dataId, nickname = it.nickname) }
                    ?: EditableNickname(),
            relations = rows.relations.map { it.toEditable() },
            events = rows.events.mapNotNull { it.toEditable() },
            photoUri = details.photoUri,
            photoChanged = false,
        )
    }

    override suspend fun createContact(form: ContactForm) = writer.createContact(form)

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        val rawContactId =
            requireNotNull(writer.loadRawContactId(contactId)) {
                "No raw contact for id=$contactId"
            }
        val rows = writer.loadContactRows(contactId)
        writer.updateContact(rawContactId, rows, form)
    }

    override suspend fun deleteContact(contactId: Long) = writer.deleteContact(contactId)
}

/** Имя для редактора: строка целевого raw contact, иначе самая младшая по raw id. */
private fun List<ExistingName>.preferredFor(rawContactId: Long?): ExistingName? =
    firstOrNull { it.rawContactId == rawContactId } ?: minByOrNull { it.rawContactId }

/**
 * Имя для редактора: структурированный режим — если в строке есть компоненты,
 * иначе одно поле с display name (с запасным значением из агрегированного имени).
 */
private fun editableNameOf(
    existing: ExistingName?,
    details: ContactDetails,
): EditableName {
    val display = existing?.display?.takeIf { it.isNotBlank() } ?: details.name
    return if (existing?.hasComponents == true) {
        EditableName(
            mode = NameMode.STRUCTURED,
            display = display,
            given = existing.given,
            family = existing.family,
            middle = existing.middle,
            prefix = existing.prefix,
            suffix = existing.suffix,
            phoneticGiven = existing.phoneticGiven,
            phoneticMiddle = existing.phoneticMiddle,
            phoneticFamily = existing.phoneticFamily,
        )
    } else {
        EditableName(mode = NameMode.DISPLAY, display = display)
    }
}

private fun ExistingPhone.toEditable(): EditablePhone =
    EditablePhone(
        dataId = dataId,
        number = number,
        kind = mapPhoneKind(type),
        customLabel = customLabelOf(type, label, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM),
    )

private fun ExistingEmail.toEditable(): EditableEmail =
    EditableEmail(
        dataId = dataId,
        address = address,
        kind = mapEmailKind(type),
        customLabel = customLabelOf(type, label, ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM),
    )

private fun ExistingAddress.toEditable(): EditableAddress =
    EditableAddress(
        dataId = dataId,
        address = address,
        kind = mapAddressKind(type),
        customLabel =
            customLabelOf(
                type,
                label,
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM,
            ),
    )

private fun ExistingInstantMessage.toEditable(): EditableInstantMessage {
    val imProtocol = mapImProtocol(protocol)
    return EditableInstantMessage(
        dataId = dataId,
        address = address,
        protocol = imProtocol,
        customProtocol = customProtocol?.takeIf { imProtocol == ImProtocol.CUSTOM && it.isNotBlank() },
    )
}

private fun ExistingRelation.toEditable(): EditableRelation =
    EditableRelation(
        dataId = dataId,
        name = name,
        kind = mapRelationKind(type),
        customLabel =
            customLabelOf(
                type,
                label,
                ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM,
            ),
    )

/** Событие без разобранной даты пропускаем: показывать и редактировать нечего. */
private fun ExistingEvent.toEditable(): EditableEvent? =
    Birthday.parse(raw)?.let { date ->
        EditableEvent(
            dataId = dataId,
            date = date,
            kind = mapEventKind(type),
            customLabel =
                customLabelOf(
                    type,
                    label,
                    ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM,
                ),
        )
    }
