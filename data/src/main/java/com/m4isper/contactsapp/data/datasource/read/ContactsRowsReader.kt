package com.m4isper.contactsapp.data.datasource.read

import com.m4isper.contactsapp.data.datasource.ContactsContractLegacy

import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.SipAddress
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website

/** Читает сырые строки контакта из курсора по [CONTACT_DATA_PROJECTION]. */
internal fun parseContactRows(cursor: Cursor): ContactRows {
    val columns = DataColumns(cursor)
    val accumulator = ContactRowsAccumulator()
    while (cursor.moveToNext()) {
        accumulator.addRow(cursor, columns)
    }
    return accumulator.toRows()
}

/** Накопитель data-строк: одна строка на элемент контакта. */
private class ContactRowsAccumulator {
    private val names = mutableListOf<ExistingName>()
    private val phones = mutableListOf<ExistingPhone>()
    private val emails = mutableListOf<ExistingEmail>()
    private val addresses = mutableListOf<ExistingAddress>()
    private val websites = mutableListOf<ExistingWebsite>()
    private val instantMessages = mutableListOf<ExistingInstantMessage>()
    private val sipAddresses = mutableListOf<ExistingSipAddress>()
    private val relations = mutableListOf<ExistingRelation>()
    private val events = mutableListOf<ExistingEvent>()
    private val groupMemberships = mutableListOf<ExistingGroupMembership>()
    private var organization: ExistingOrganization? = null
    private var note: ExistingNote? = null
    private var birthday: ExistingBirthday? = null
    private var nickname: ExistingNickname? = null

    fun addRow(
        cursor: Cursor,
        columns: DataColumns,
    ) {
        val dataId = cursor.getLong(columns.id)
        val rawId = cursor.getLong(columns.raw)
        val data1 = cursor.getString(columns.data1).orEmpty()
        when (val mime = cursor.getString(columns.mime)) {
            StructuredName.CONTENT_ITEM_TYPE ->
                names += existingName(cursor, columns, dataId, rawId)
            Phone.CONTENT_ITEM_TYPE,
            Email.CONTENT_ITEM_TYPE,
            StructuredPostal.CONTENT_ITEM_TYPE,
            Website.CONTENT_ITEM_TYPE,
            ContactsContractLegacy.Sip.CONTENT_ITEM_TYPE,
            -> addCommunicationRow(mime, cursor, columns, dataId, rawId, data1)
            ContactsContractLegacy.Im.CONTENT_ITEM_TYPE -> addInstantMessageRow(cursor, columns, dataId, rawId, data1)
            Relation.CONTENT_ITEM_TYPE -> relations += existingRelation(cursor, columns, dataId, rawId)
            Event.CONTENT_ITEM_TYPE -> addEventRow(cursor, columns, dataId, rawId, data1)
            GroupMembership.CONTENT_ITEM_TYPE ->
                data1.toLongOrNull()?.let { groupId ->
                    groupMemberships += ExistingGroupMembership(dataId, rawId, groupId)
                }
            else -> addSingleRow(cursor, columns, dataId, rawId, data1)
        }
    }

    private fun addCommunicationRow(
        mime: String,
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawId: Long,
        data1: String,
    ) {
        when (mime) {
            Phone.CONTENT_ITEM_TYPE -> addPhoneRow(cursor, columns, dataId, rawId, data1)
            Email.CONTENT_ITEM_TYPE -> addEmailRow(cursor, columns, dataId, rawId, data1)
            StructuredPostal.CONTENT_ITEM_TYPE -> addresses += existingAddress(cursor, columns, dataId, rawId)
            Website.CONTENT_ITEM_TYPE -> addWebsiteRow(cursor, columns, dataId, rawId, data1)
            ContactsContractLegacy.Sip.CONTENT_ITEM_TYPE -> addSipRow(cursor, columns, dataId, rawId, data1)
        }
    }

    private fun addPhoneRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawId: Long,
        data1: String,
    ) {
        phones +=
            ExistingPhone(
                dataId = dataId,
                rawContactId = rawId,
                number = data1,
                type = cursor.getInt(columns.data2),
                label = cursor.getString(columns.data3),
                isPrimary = cursor.getInt(columns.isPrimary) == 1,
            )
    }

    private fun addEmailRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawId: Long,
        data1: String,
    ) {
        emails +=
            ExistingEmail(
                dataId = dataId,
                rawContactId = rawId,
                address = data1,
                type = cursor.getInt(columns.data2),
                label = cursor.getString(columns.data3),
                isPrimary = cursor.getInt(columns.isPrimary) == 1,
            )
    }

    private fun addWebsiteRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawId: Long,
        data1: String,
    ) {
        websites +=
            ExistingWebsite(
                dataId = dataId,
                rawContactId = rawId,
                url = data1,
                type = cursor.getInt(columns.data2),
                label = cursor.getString(columns.data3),
            )
    }

    private fun addSipRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawId: Long,
        data1: String,
    ) {
        sipAddresses +=
            ExistingSipAddress(
                dataId = dataId,
                rawContactId = rawId,
                address = data1,
                type = cursor.getInt(columns.data2),
                label = cursor.getString(columns.data3),
            )
    }

    private fun addInstantMessageRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawId: Long,
        data1: String,
    ) {
        instantMessages +=
            ExistingInstantMessage(
                dataId = dataId,
                rawContactId = rawId,
                address = data1,
                type = cursor.getInt(columns.data2),
                protocol = cursor.getString(columns.data5)?.toIntOrNull(),
                customProtocol = cursor.getString(columns.data6),
            )
    }

    /** Одиночные поля: организация, заметка и никнейм — храним первую строку. */
    private fun addSingleRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawContactId: Long,
        data1: String,
    ) {
        when (cursor.getString(columns.mime)) {
            Organization.CONTENT_ITEM_TYPE ->
                if (organization == null) organization = existingOrganization(cursor, columns, dataId, rawContactId)
            Note.CONTENT_ITEM_TYPE ->
                if (note == null) note = ExistingNote(dataId, rawContactId, data1)
            Nickname.CONTENT_ITEM_TYPE ->
                if (nickname == null) nickname = ExistingNickname(dataId, rawContactId, data1)
        }
    }

    fun toRows(): ContactRows =
        ContactRows(
            names = names,
            phones = phones,
            emails = emails,
            addresses = addresses,
            websites = websites,
            instantMessages = instantMessages,
            sipAddresses = sipAddresses,
            relations = relations,
            events = events,
            organization = organization,
            note = note,
            birthday = birthday,
            nickname = nickname,
            groupMemberships = groupMemberships,
        )

    /** День рождения хранится тем же mimetype Event, что и прочие даты. */
    private fun addEventRow(
        cursor: Cursor,
        columns: DataColumns,
        dataId: Long,
        rawContactId: Long,
        data1: String,
    ) {
        val type = cursor.getInt(columns.data2)
        if (type == Event.TYPE_BIRTHDAY) {
            if (birthday == null) birthday = ExistingBirthday(dataId, rawContactId, data1)
        } else {
            events += ExistingEvent(dataId, rawContactId, data1, type, cursor.getString(columns.data3))
        }
    }
}

private fun existingName(
    cursor: Cursor,
    columns: DataColumns,
    dataId: Long,
    rawContactId: Long,
): ExistingName =
    ExistingName(
        dataId = dataId,
        rawContactId = rawContactId,
        display = cursor.getString(columns.data1).orEmpty(),
        given = cursor.getString(columns.data2).orEmpty(),
        family = cursor.getString(columns.data3).orEmpty(),
        prefix = cursor.getString(columns.data4).orEmpty(),
        middle = cursor.getString(columns.data5).orEmpty(),
        suffix = cursor.getString(columns.data6).orEmpty(),
        phoneticGiven = cursor.getString(columns.data7).orEmpty(),
        phoneticMiddle = cursor.getString(columns.data8).orEmpty(),
        phoneticFamily = cursor.getString(columns.data9).orEmpty(),
    )

private fun existingOrganization(
    cursor: Cursor,
    columns: DataColumns,
    dataId: Long,
    rawContactId: Long,
): ExistingOrganization =
    ExistingOrganization(
        dataId = dataId,
        rawContactId = rawContactId,
        company = cursor.getString(columns.data1).orEmpty(),
        title = cursor.getString(columns.data4).orEmpty(),
        department = cursor.getString(columns.data5).orEmpty(),
        jobDescription = cursor.getString(columns.data6).orEmpty(),
        officeLocation = cursor.getString(columns.data7).orEmpty(),
    )

private fun existingAddress(
    cursor: Cursor,
    columns: DataColumns,
    dataId: Long,
    rawContactId: Long,
): ExistingAddress =
    ExistingAddress(
        dataId = dataId,
        rawContactId = rawContactId,
        address = cursor.getString(columns.data1).orEmpty(),
        type = cursor.getInt(columns.data2),
        label = cursor.getString(columns.data3),
        street = cursor.getString(columns.data4).orEmpty(),
        poBox = cursor.getString(columns.data5).orEmpty(),
        neighborhood = cursor.getString(columns.data6).orEmpty(),
        city = cursor.getString(columns.data7).orEmpty(),
        region = cursor.getString(columns.data8).orEmpty(),
        postcode = cursor.getString(columns.data9).orEmpty(),
        country = cursor.getString(columns.data10).orEmpty(),
    )

private fun existingRelation(
    cursor: Cursor,
    columns: DataColumns,
    dataId: Long,
    rawContactId: Long,
): ExistingRelation =
    ExistingRelation(
        dataId = dataId,
        rawContactId = rawContactId,
        name = cursor.getString(columns.data1).orEmpty(),
        type = cursor.getInt(columns.data2),
        label = cursor.getString(columns.data3),
    )
