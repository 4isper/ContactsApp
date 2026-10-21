package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails

/** Чтение контактов: список, один контакт и доступные аккаунты. */
interface ContactsDataSource {
    suspend fun loadContacts(): List<ContactDetails>

    suspend fun loadContact(contactId: Long): ContactDetails?

    /** Аккаунт целевого raw contact; локальный, если raw contact не найден. */
    suspend fun loadContactAccount(contactId: Long): ContactAccount = ContactAccount.Local

    /** Аккаунты, в которые можно сохранить контакт; локальный есть всегда. */
    suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

    /** Фото контакта для экспорта; null, если фото нет. */
    suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    /** id контакта «Мой профиль» из провайдера; null, если профиля нет. */
    suspend fun loadProfileContactId(): Long? = null
}
