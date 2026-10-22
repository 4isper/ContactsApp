package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactAccount

/** Аккаунты контактов: локальный и синхронизируемые. */
interface ContactsAccounts {
    /** Аккаунт существующего контакта. */
    suspend fun getContactAccount(contactId: Long): ContactAccount

    /** Аккаунты для выбора при создании; локальный аккаунт есть всегда. */
    suspend fun getContactAccounts(): List<ContactAccount>
}
