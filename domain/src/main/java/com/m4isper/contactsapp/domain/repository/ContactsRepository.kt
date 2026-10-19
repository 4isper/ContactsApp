package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm

interface ContactsRepository {
    suspend fun getContacts(): List<ContactDetails>

    suspend fun getContactDetails(contactId: Long): ContactDetails?

    suspend fun getContactForm(contactId: Long): ContactForm?

    suspend fun createContact(form: ContactForm)

    suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    )

    suspend fun deleteContact(contactId: Long)

    /** Слияние дубликатов: данные source-контактов переносятся в target, источники удаляются. */
    suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    )
}
