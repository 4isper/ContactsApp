package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.ContactDetails

/** Чтение контактов: список и один контакт. */
interface ContactsDataSource {
    suspend fun loadContacts(): List<ContactDetails>

    suspend fun loadContact(contactId: Long): ContactDetails?
}
