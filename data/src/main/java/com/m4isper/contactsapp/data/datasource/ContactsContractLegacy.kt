package com.m4isper.contactsapp.data.datasource

/**
 * Замороженные значения устаревших полей ContactsContract.
 *
 * Эти константы стабильны и не меняются между версиями Android. Использование
 * локальных значений позволяет не ссылаться на deprecated-классы
 * ContactsContract.CommonDataKinds.SipAddress и Im напрямую.
 */
object ContactsContractLegacy {
    const val LAST_TIME_CONTACTED = "last_time_contacted"

    object Sip {
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/sip_address"
        const val SIP_ADDRESS = "data1"
        const val TYPE = "data2"
        const val LABEL = "data3"
        const val TYPE_CUSTOM = 0
        const val TYPE_HOME = 1
        const val TYPE_WORK = 2
        const val TYPE_OTHER = 3
    }

    object Im {
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/im"
        const val DATA = "data1"
        const val TYPE = "data2"
        const val PROTOCOL = "data5"
        const val CUSTOM_PROTOCOL = "data6"
        const val TYPE_HOME = 1
        const val TYPE_WORK = 2
        const val TYPE_OTHER = 3
        const val PROTOCOL_AIM = 0
        const val PROTOCOL_MSN = 1
        const val PROTOCOL_YAHOO = 2
        const val PROTOCOL_SKYPE = 3
        const val PROTOCOL_QQ = 4
        const val PROTOCOL_GOOGLE_TALK = 5
        const val PROTOCOL_ICQ = 6
        const val PROTOCOL_JABBER = 7
        const val PROTOCOL_CUSTOM = -1
    }
}
