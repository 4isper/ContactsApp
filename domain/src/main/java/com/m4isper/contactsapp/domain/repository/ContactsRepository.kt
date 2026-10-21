// Подавление осознанное: порт описывает всё, чем умеет работать репозиторий контактов.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm

interface ContactsRepository {
    suspend fun getContacts(): List<ContactDetails>

    suspend fun getContactDetails(contactId: Long): ContactDetails?

    suspend fun getContactForm(contactId: Long): ContactForm?

    /** Аккаунт существующего контакта; локальный, если данные недоступны. */
    suspend fun getContactAccount(contactId: Long): ContactAccount =
        getContactDetails(contactId)?.account ?: ContactAccount.Local

    /** Аккаунты для выбора при создании; локальный аккаунт есть всегда. */
    suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

    suspend fun createContact(form: ContactForm): Long

    /** Создание с явным выбором аккаунта; по умолчанию — локальный. @return id нового контакта. */
    suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long = createContact(form)

    suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    )

    suspend fun deleteContact(contactId: Long)

    /** Заменяет фото контакта; байты уже уменьшены и сжаты в JPEG. */
    suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    )

    /** Фото контакта для экспорта; null, если фото нет. */
    suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    /** Слияние дубликатов: данные source-контактов переносятся в target, источники удаляются. */
    suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    )
}
