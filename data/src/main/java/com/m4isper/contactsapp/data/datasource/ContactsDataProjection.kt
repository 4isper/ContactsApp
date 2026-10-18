package com.m4isper.contactsapp.data.datasource

import android.database.Cursor
import android.provider.ContactsContract

/**
 * Единая проекция строк таблицы Data: используется и агрегацией контактов,
 * и чтением строк формы, чтобы наборы колонок не расходились.
 *
 * data1..data9 — физические колонки, в которых лежат все нужные типы
 * (телефон/email — значение в data1, тип в data2, метка в data3;
 * организация — company в data1, title в data4;
 * StructuredName — display в data1, given в data2, family в data3,
 * prefix в data4, middle в data5, suffix в data6,
 * фонетика — given в data7, middle в data8, family в data9).
 */
internal val CONTACT_DATA_PROJECTION =
    arrayOf(
        ContactsContract.Data._ID,
        ContactsContract.Data.CONTACT_ID,
        ContactsContract.Data.RAW_CONTACT_ID,
        ContactsContract.Data.MIMETYPE,
        ContactsContract.Data.DATA1,
        ContactsContract.Data.DATA2,
        ContactsContract.Data.DATA3,
        ContactsContract.Data.DATA4,
        ContactsContract.Data.DATA5,
        ContactsContract.Data.DATA6,
        ContactsContract.Data.DATA7,
        ContactsContract.Data.DATA8,
        ContactsContract.Data.DATA9,
    )

/** Индексы колонок [CONTACT_DATA_PROJECTION]: считаются один раз на курсор. */
internal class DataColumns(
    cursor: Cursor,
) {
    val id: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)
    val contactId: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
    val raw: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID)
    val mime: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
    val data1: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
    val data2: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
    val data3: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
    val data4: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA4)
    val data5: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA5)
    val data6: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA6)
    val data7: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA7)
    val data8: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA8)
    val data9: Int = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA9)
}
