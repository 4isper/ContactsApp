package com.m4isper.contactsapp.data.datasource

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Im
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.ImProtocol

internal fun imProtocolInt(protocol: ImProtocol): Int =
    when (protocol) {
        ImProtocol.AIM -> Im.PROTOCOL_AIM
        ImProtocol.MSN -> Im.PROTOCOL_MSN
        ImProtocol.YAHOO -> Im.PROTOCOL_YAHOO
        ImProtocol.SKYPE -> Im.PROTOCOL_SKYPE
        ImProtocol.QQ -> Im.PROTOCOL_QQ
        ImProtocol.GOOGLE_TALK -> Im.PROTOCOL_GOOGLE_TALK
        ImProtocol.ICQ -> Im.PROTOCOL_ICQ
        ImProtocol.JABBER -> Im.PROTOCOL_JABBER
        ImProtocol.CUSTOM -> Im.PROTOCOL_CUSTOM
    }

/**
 * Тип (дом/работа) не редактируем: новые строки пишем как OTHER,
 * существующим при update передаём исходный [type], чтобы не потерять его.
 */
internal fun instantMessageValues(
    im: EditableInstantMessage,
    type: Int = Im.TYPE_OTHER,
): ContentValues =
    ContentValues().apply {
        put(Im.DATA, im.address)
        put(Im.TYPE, type)
        put(Im.PROTOCOL, imProtocolInt(im.protocol))
        put(
            Im.CUSTOM_PROTOCOL,
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
        dataIdOf = { it.dataId },
        insert = { im ->
            dataInsert(Im.CONTENT_ITEM_TYPE, rawContactId = rawContactId)
                .withValues(instantMessageValues(im))
        },
        update = { dataId, im ->
            dataUpdate(dataId).withValues(instantMessageValues(im, typeByDataId[dataId] ?: Im.TYPE_OTHER))
        },
    )
}
