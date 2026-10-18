package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.domain.model.ContactForm

/** Запись контактов и чтение сырых строк для diff при обновлении. */
interface ContactsWriterDataSource {
    suspend fun createContact(form: ContactForm)

    suspend fun updateContact(
        rawContactId: Long,
        rows: ContactRows,
        form: ContactForm,
    )

    suspend fun loadContactRows(contactId: Long): ContactRows

    suspend fun loadRawContactId(contactId: Long): Long?

    suspend fun deleteContact(contactId: Long)
}
