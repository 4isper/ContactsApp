package com.m4isper.contactsapp.data.repository

import android.provider.ContactsContract
import com.m4isper.contactsapp.data.datasource.ContactsDataSource
import com.m4isper.contactsapp.data.datasource.ContactsWriterDataSource
import com.m4isper.contactsapp.data.datasource.read.ExistingAddress
import com.m4isper.contactsapp.data.datasource.read.ExistingEmail
import com.m4isper.contactsapp.data.datasource.read.ExistingInstantMessage
import com.m4isper.contactsapp.data.datasource.read.ExistingName
import com.m4isper.contactsapp.data.datasource.read.ExistingPhone
import com.m4isper.contactsapp.data.datasource.read.ExistingRelation
import com.m4isper.contactsapp.data.datasource.read.customLabelOf
import com.m4isper.contactsapp.data.datasource.read.editableEventOf
import com.m4isper.contactsapp.data.datasource.read.mapAddressKind
import com.m4isper.contactsapp.data.datasource.read.mapEmailKind
import com.m4isper.contactsapp.data.datasource.read.mapImProtocol
import com.m4isper.contactsapp.data.datasource.read.mapPhoneKind
import com.m4isper.contactsapp.data.datasource.read.mapRelationKind
import com.m4isper.contactsapp.data.datasource.write.mergeContactForms
import com.m4isper.contactsapp.data.format.birthdayFromString
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableAddress
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
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

    override suspend fun getContactAccount(contactId: Long): ContactAccount = dataSource.loadContactAccount(contactId)

    override suspend fun getContactForm(contactId: Long): ContactForm? {
        val details = dataSource.loadContact(contactId) ?: return null
        val rows = writer.loadContactRows(contactId)
        val rawContactId = writer.loadRawContactId(contactId)
        return ContactForm(
            name = editableNameOf(rows.names.preferredFor(rawContactId), details),
            phones = rows.phones.map { it.toEditable() },
            emails = rows.emails.map { it.toEditable() },
            addresses = rows.addresses.map { it.toEditable() },
            websites = rows.websites.map { EditableWebsite(rowId = it.dataId, url = it.url) },
            instantMessages = rows.instantMessages.map { it.toEditable() },
            organization =
                rows.organization
                    ?.let { EditableOrganization(rowId = it.dataId, company = it.company, title = it.title) }
                    ?: EditableOrganization(),
            note = rows.note?.let { EditableNote(rowId = it.dataId, note = it.note) } ?: EditableNote(),
            birthday =
                rows.birthday
                    ?.let { EditableBirthday(rowId = it.dataId, value = birthdayFromString(it.raw)) }
                    ?: EditableBirthday(),
            nickname =
                rows.nickname
                    ?.let { EditableNickname(rowId = it.dataId, nickname = it.nickname) }
                    ?: EditableNickname(),
            relations = rows.relations.map { it.toEditable() },
            events = rows.events.map(::editableEventOf),
            groups = rows.groupMemberships.map { it.groupId },
            photoUri = details.photoUri,
            photoChanged = false,
        )
    }

    override suspend fun getContactAccounts(): List<ContactAccount> = dataSource.getContactAccounts()

    override suspend fun createContact(form: ContactForm) = writer.createContact(form, ContactAccount.Local)

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ) = writer.createContact(form, account)

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

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ) {
        if (sourceIds.isEmpty()) return
        val sources =
            sourceIds
                .filter { it != targetId }
                .mapNotNull { id -> getContactForm(id)?.let { id to it } }
        if (sources.isEmpty()) return
        val targetForm =
            requireNotNull(getContactForm(targetId)) {
                "No contact form for id=$targetId"
            }
        updateContact(targetId, mergeContactForms(targetForm, sources.map { it.second }))
        sources.forEach { (id, _) -> deleteContact(id) }
    }
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
        rowId = dataId,
        number = number,
        kind = mapPhoneKind(type),
        customLabel = customLabelOf(type, label, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM),
    )

private fun ExistingEmail.toEditable(): EditableEmail =
    EditableEmail(
        rowId = dataId,
        address = address,
        kind = mapEmailKind(type),
        customLabel = customLabelOf(type, label, ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM),
    )

private fun ExistingAddress.toEditable(): EditableAddress =
    EditableAddress(
        rowId = dataId,
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
        rowId = dataId,
        address = address,
        protocol = imProtocol,
        customProtocol = customProtocol?.takeIf { imProtocol == ImProtocol.CUSTOM && it.isNotBlank() },
    )
}

private fun ExistingRelation.toEditable(): EditableRelation =
    EditableRelation(
        rowId = dataId,
        name = name,
        kind = mapRelationKind(type),
        customLabel =
            customLabelOf(
                type,
                label,
                ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM,
            ),
    )
