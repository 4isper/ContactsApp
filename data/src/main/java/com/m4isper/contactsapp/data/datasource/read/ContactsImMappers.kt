package com.m4isper.contactsapp.data.datasource.read

import android.provider.ContactsContract.CommonDataKinds.Im
import com.m4isper.contactsapp.data.datasource.ContactsContractLegacy
import com.m4isper.contactsapp.domain.model.contact.ImProtocol
import com.m4isper.contactsapp.domain.model.contact.InstantMessage

/**
 * Протокол лежит в data5, произвольное название сервиса — в data6.
 * Неизвестный или отсутствующий протокол трактуем как произвольный.
 */
internal fun mapImProtocol(raw: Int?): ImProtocol =
    when (raw) {
        ContactsContractLegacy.Im.PROTOCOL_AIM -> ImProtocol.AIM
        ContactsContractLegacy.Im.PROTOCOL_MSN -> ImProtocol.MSN
        ContactsContractLegacy.Im.PROTOCOL_YAHOO -> ImProtocol.YAHOO
        ContactsContractLegacy.Im.PROTOCOL_SKYPE -> ImProtocol.SKYPE
        ContactsContractLegacy.Im.PROTOCOL_QQ -> ImProtocol.QQ
        ContactsContractLegacy.Im.PROTOCOL_GOOGLE_TALK -> ImProtocol.GOOGLE_TALK
        ContactsContractLegacy.Im.PROTOCOL_ICQ -> ImProtocol.ICQ
        ContactsContractLegacy.Im.PROTOCOL_JABBER -> ImProtocol.JABBER
        else -> ImProtocol.CUSTOM
    }

internal fun instantMessagesOf(rows: List<DataRow>): List<InstantMessage> =
    rows
        .filter { it.mimetype == ContactsContractLegacy.Im.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            row.data1?.trim()?.takeIf { it.isNotEmpty() }?.let { address ->
                val protocol = mapImProtocol(row.data5?.toIntOrNull())
                InstantMessage(
                    address = address,
                    protocol = protocol,
                    customProtocol =
                        row.data6
                            ?.trim()
                            ?.takeIf { protocol == ImProtocol.CUSTOM && it.isNotEmpty() },
                )
            }
        }.distinctBy { it.address }
