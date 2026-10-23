package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm

/**
 * Чтение контактов: список, страница, карточка, форма редактора и фото.
 * Порт описывает только контракт: никакой логики и запасных реализаций.
 */
interface ContactsReader {
    suspend fun getContacts(): List<ContactDetails>

    /** Страница контактов: список появляется по мере загрузки. */
    suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails>

    suspend fun getContactDetails(contactId: Long): ContactDetails?

    suspend fun getContactForm(contactId: Long): ContactForm?

    /** Фото контакта для экспорта; null, если фото нет. */
    suspend fun loadContactPhoto(contactId: Long): ByteArray?

    /** id контакта «Мой профиль»; null, если профиля нет. */
    suspend fun getProfileContactId(): Long?
}
