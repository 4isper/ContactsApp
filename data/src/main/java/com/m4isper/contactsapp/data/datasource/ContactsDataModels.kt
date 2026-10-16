package com.m4isper.contactsapp.data.datasource

/** Шапка контакта из таблицы Contacts: одна строка на контакт. */
internal data class ContactHeaderRow(
    val contactId: Long,
    val name: String,
    val photoUri: String? = null,
)

/** Сырая data-строка: одна на любой элемент контакта (номер, email, адрес...). */
internal data class DataRow(
    val contactId: Long,
    val mimetype: String,
    val data1: String?,
    val data2: Int = 0,
    val data3: String? = null,
    val data4: String? = null,
)
