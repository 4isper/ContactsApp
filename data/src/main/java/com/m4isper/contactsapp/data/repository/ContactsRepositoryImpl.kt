// Подавление осознанное: мелкие mapping-функции держим рядом с репозиторием.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.data.repository

import android.provider.ContactsContract
import com.m4isper.contactsapp.data.datasource.ContactsDataSource
import com.m4isper.contactsapp.data.datasource.ContactsWriterDataSource
import com.m4isper.contactsapp.data.datasource.read.ExistingAddress
import com.m4isper.contactsapp.data.datasource.read.ExistingBirthday
import com.m4isper.contactsapp.data.datasource.read.ExistingEmail
import com.m4isper.contactsapp.data.datasource.read.ExistingInstantMessage
import com.m4isper.contactsapp.data.datasource.read.ExistingName
import com.m4isper.contactsapp.data.datasource.read.ExistingNickname
import com.m4isper.contactsapp.data.datasource.read.ExistingNote
import com.m4isper.contactsapp.data.datasource.read.ExistingOrganization
import com.m4isper.contactsapp.data.datasource.read.ExistingPhone
import com.m4isper.contactsapp.data.datasource.read.ExistingRelation
import com.m4isper.contactsapp.data.datasource.read.ExistingSipAddress
import com.m4isper.contactsapp.data.datasource.read.ExistingWebsite
import com.m4isper.contactsapp.data.datasource.read.customLabelOf
import com.m4isper.contactsapp.data.datasource.read.editableEventOf
import com.m4isper.contactsapp.data.datasource.read.mapAddressKind
import com.m4isper.contactsapp.data.datasource.read.mapEmailKind
import com.m4isper.contactsapp.data.datasource.read.mapImProtocol
import com.m4isper.contactsapp.data.datasource.read.mapPhoneKind
import com.m4isper.contactsapp.data.datasource.read.mapRelationKind
import com.m4isper.contactsapp.data.datasource.read.mapSipKind
import com.m4isper.contactsapp.data.datasource.read.mapWebsiteKind
import com.m4isper.contactsapp.data.format.birthdayFromString
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactLink
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
import com.m4isper.contactsapp.domain.model.EditableSipAddress
import com.m4isper.contactsapp.domain.model.EditableWebsite
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ContactsReader>())
@ContributesBinding(AppScope::class, binding = binding<ContactsAccounts>())
@ContributesBinding(AppScope::class, binding = binding<ContactsWriter>())
@ContributesBinding(AppScope::class, binding = binding<ContactsMerger>())
class ContactsRepositoryImpl(
    private val dataSource: ContactsDataSource,
    private val writer: ContactsWriterDataSource,
) : ContactsReader,
    ContactsAccounts,
    ContactsWriter,
    ContactsMerger {
    override suspend fun getContacts(): List<ContactDetails> = dataSource.loadContacts()

    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = dataSource.loadContactsPage(offset, limit)

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
            websites = rows.websites.map { it.toEditable() },
            instantMessages = rows.instantMessages.map { it.toEditable() },
            sipAddresses = rows.sipAddresses.map { it.toEditable() },
            organization = rows.organization?.toEditable() ?: EditableOrganization(),
            note = rows.note?.toEditable() ?: EditableNote(),
            birthday = rows.birthday?.toEditable() ?: EditableBirthday(),
            nickname = rows.nickname?.toEditable() ?: EditableNickname(),
            relations = rows.relations.map { it.toEditable() },
            events = rows.events.map(::editableEventOf),
            groups = rows.groupMemberships.map { it.groupId },
            photoUri = details.photoUri,
            photoChanged = false,
        )
    }

    override suspend fun getContactAccounts(): List<ContactAccount> = dataSource.getContactAccounts()

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long = writer.createContact(form, account)

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

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = writer.setContactPhoto(contactId, photo)

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = dataSource.loadContactPhoto(contactId)

    override suspend fun getProfileContactId(): Long? = dataSource.loadProfileContactId()

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): ContactLink? {
        val targetRawId = writer.loadRawContactId(targetId)
        val sourceRawIds =
            sourceIds
                .filter { it != targetId }
                .mapNotNull { id -> writer.loadRawContactId(id) }
        return if (targetRawId != null && sourceRawIds.isNotEmpty()) {
            writer.linkRawContacts(targetRawId, sourceRawIds)
            ContactLink(targetRawContactId = targetRawId, sourceRawContactIds = sourceRawIds)
        } else {
            null
        }
    }

    override suspend fun unmergeContacts(link: ContactLink) {
        writer.unlinkRawContacts(link.targetRawContactId, link.sourceRawContactIds)
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

private fun ExistingOrganization.toEditable(): EditableOrganization =
    EditableOrganization(
        rowId = dataId,
        company = company,
        title = title,
        department = department,
        jobDescription = jobDescription,
        officeLocation = officeLocation,
    )

private fun ExistingNote.toEditable(): EditableNote = EditableNote(rowId = dataId, note = note)

private fun ExistingBirthday.toEditable(): EditableBirthday =
    EditableBirthday(rowId = dataId, value = birthdayFromString(raw))

private fun ExistingNickname.toEditable(): EditableNickname = EditableNickname(rowId = dataId, nickname = nickname)

private fun ExistingPhone.toEditable(): EditablePhone =
    EditablePhone(
        rowId = dataId,
        number = number,
        kind = mapPhoneKind(type),
        customLabel = customLabelOf(type, label, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM),
        isPrimary = isPrimary,
    )

private fun ExistingEmail.toEditable(): EditableEmail =
    EditableEmail(
        rowId = dataId,
        address = address,
        kind = mapEmailKind(type),
        customLabel = customLabelOf(type, label, ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM),
        isPrimary = isPrimary,
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
        street = street,
        poBox = poBox,
        neighborhood = neighborhood,
        city = city,
        region = region,
        postcode = postcode,
        country = country,
    )

private fun ExistingWebsite.toEditable(): EditableWebsite =
    EditableWebsite(
        rowId = dataId,
        url = url,
        kind = mapWebsiteKind(type),
        customLabel =
            customLabelOf(
                type,
                label,
                ContactsContract.CommonDataKinds.Website.TYPE_CUSTOM,
            ),
    )

private fun ExistingSipAddress.toEditable(): EditableSipAddress =
    EditableSipAddress(
        rowId = dataId,
        address = address,
        kind = mapSipKind(type),
        customLabel =
            customLabelOf(
                type,
                label,
                ContactsContract.CommonDataKinds.SipAddress.TYPE_CUSTOM,
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
