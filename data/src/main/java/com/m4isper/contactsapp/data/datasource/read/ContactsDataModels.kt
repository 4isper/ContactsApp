package com.m4isper.contactsapp.data.datasource.read

/** Шапка контакта из таблицы Contacts: одна строка на контакт. */
internal data class ContactHeaderRow(
    val contactId: Long,
    val name: String,
    val photoUri: String? = null,
    val lastUpdated: Long? = null,
    val lastContacted: Long? = null,
    val timesContacted: Int = 0,
)

/** Сырая data-строка: одна на любой элемент контакта (номер, email, адрес...). */
internal data class DataRow(
    val contactId: Long,
    val mimetype: String,
    val data1: String?,
    val data2: String? = null,
    val data3: String? = null,
    val data4: String? = null,
    val data5: String? = null,
    val data6: String? = null,
    val data7: String? = null,
    val data8: String? = null,
    val data9: String? = null,
) {
    /** Числовое значение data2: тип телефона/email/адреса или тип события. */
    val data2Int: Int
        get() = data2?.toIntOrNull() ?: 0
}
