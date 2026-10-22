package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactForm

/** Запись контактов и чтение сырых строк для diff при обновлении. */
interface ContactsWriterDataSource {
    /** Создаёт контакт. @return id созданного контакта. */
    suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long

    suspend fun updateContact(
        rawContactId: Long,
        rows: ContactRows,
        form: ContactForm,
    )

    suspend fun loadContactRows(contactId: Long): ContactRows

    suspend fun loadRawContactId(contactId: Long): Long?

    suspend fun deleteContact(contactId: Long)

    /** Заменяет фото контакта готовыми байтами. */
    suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    )

    /** Связывает raw-контакты: они агрегируются в один контакт. */
    suspend fun linkRawContacts(
        targetRawContactId: Long,
        sourceRawContactIds: List<Long>,
    )

    /** Разрывает связь: контакты снова видны отдельно. */
    suspend fun unlinkRawContacts(
        targetRawContactId: Long,
        sourceRawContactIds: List<Long>,
    )
}
