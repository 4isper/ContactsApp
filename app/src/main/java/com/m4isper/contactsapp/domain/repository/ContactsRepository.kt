package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactDetails

interface ContactsRepository {
    suspend fun getContacts(): List<ContactDetails>

    suspend fun getContactDetails(contactId: Long): ContactDetails?
}
