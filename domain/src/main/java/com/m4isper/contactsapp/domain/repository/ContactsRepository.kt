// Подавление осознанное: порт описывает всё, чем умеет работать репозиторий контактов.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactLink

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

    /**
     * Слияние дубликатов: источники связываются с целевым и становятся его частью.
     * @return связь для отмены слияния.
     */
    suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): ContactLink?

    /** Отменяет слияние: контакты снова видны отдельно. */
    suspend fun unmergeContacts(link: ContactLink) = Unit

    /** id контакта «Мой профиль»; null, если профиля нет. */
    suspend fun getProfileContactId(): Long? = null

    /**
     * Копирует контакт в другой аккаунт: создаёт копию формы.
     * @return id нового контакта.
     */
    suspend fun copyContact(
        contactId: Long,
        account: ContactAccount,
    ): Long {
        val form = getContactForm(contactId) ?: error("no contact form for id=$contactId")
        return createContact(form, account)
    }

    /**
     * Переносит контакт в другой аккаунт: создаёт копию и удаляет исходный.
     * @return id нового контакта.
     */
    suspend fun moveContact(
        contactId: Long,
        account: ContactAccount,
    ): Long {
        val newId = copyContact(contactId, account)
        deleteContact(contactId)
        return newId
    }
}
