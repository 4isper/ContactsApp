package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails

/** Чтение контактов: список, один контакт и доступные аккаунты. */
interface ContactsDataSource {
    suspend fun loadContacts(): List<ContactDetails>

    suspend fun loadContact(contactId: Long): ContactDetails?

    /** Аккаунты, в которые можно сохранить контакт; локальный есть всегда. */
    suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)
}
