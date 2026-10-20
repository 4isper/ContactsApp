package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm

interface ContactsRepository {
    suspend fun getContacts(): List<ContactDetails>

    suspend fun getContactDetails(contactId: Long): ContactDetails?

    suspend fun getContactForm(contactId: Long): ContactForm?

    /** Аккаунты для выбора при создании; локальный аккаунт есть всегда. */
    suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

    suspend fun createContact(form: ContactForm)

    /** Создание с явным выбором аккаунта; по умолчанию — локальный. */
    suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ) = createContact(form)

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
