package com.m4isper.contactsapp.data.datasource.write.contacts

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Im
import com.m4isper.contactsapp.data.datasource.ContactsContractLegacy
import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.ImProtocol

internal fun imProtocolInt(protocol: ImProtocol): Int =
    when (protocol) {
        ImProtocol.AIM -> ContactsContractLegacy.Im.PROTOCOL_AIM
        ImProtocol.MSN -> ContactsContractLegacy.Im.PROTOCOL_MSN
        ImProtocol.YAHOO -> ContactsContractLegacy.Im.PROTOCOL_YAHOO
        ImProtocol.SKYPE -> ContactsContractLegacy.Im.PROTOCOL_SKYPE
        ImProtocol.QQ -> ContactsContractLegacy.Im.PROTOCOL_QQ
        ImProtocol.GOOGLE_TALK -> ContactsContractLegacy.Im.PROTOCOL_GOOGLE_TALK
        ImProtocol.ICQ -> ContactsContractLegacy.Im.PROTOCOL_ICQ
        ImProtocol.JABBER -> ContactsContractLegacy.Im.PROTOCOL_JABBER
        ImProtocol.CUSTOM -> ContactsContractLegacy.Im.PROTOCOL_CUSTOM
    }

/**
 * Тип (дом/работа) не редактируем: новые строки пишем как OTHER,
 * существующим при update передаём исходный [type], чтобы не потерять его.
 */
internal fun instantMessageValues(
    im: EditableInstantMessage,
    type: Int = ContactsContractLegacy.Im.TYPE_OTHER,
): ContentValues =
    ContentValues().apply {
        put(ContactsContractLegacy.Im.DATA, im.address)
        put(ContactsContractLegacy.Im.TYPE, type)
        put(ContactsContractLegacy.Im.PROTOCOL, imProtocolInt(im.protocol))
        put(
            ContactsContractLegacy.Im.CUSTOM_PROTOCOL,
            im.customProtocol.takeIf { im.protocol == ImProtocol.CUSTOM && !it.isNullOrBlank() },
        )
    }

/** Мессенджеры — отдельная diff-группа, чтобы listOperations не разрастался. */
internal fun imOperations(
    rawContactId: Long,
    rows: ContactRows,
    form: ContactForm,
): List<ContentProviderOperation> {
    val typeByDataId = rows.instantMessages.associate { it.dataId to it.type }
    return diffListOperations(
        newItems = form.instantMessages.filter { it.address.isNotBlank() },
        existingIds = rows.instantMessages.mapTo(mutableSetOf()) { it.dataId },
        dataIdOf = { it.rowId },
        insert = { im ->
            dataInsert(ContactsContractLegacy.Im.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(instantMessageValues(im))
        },
        update = { dataId, im ->
            dataUpdate(dataId).withValues(
                instantMessageValues(im, typeByDataId[dataId] ?: ContactsContractLegacy.Im.TYPE_OTHER),
            )
        },
    )
}
