package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.Contact

interface ContactsRepository {
    suspend fun getContacts(): List<Contact>
}
