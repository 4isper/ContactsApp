package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactForm

/** Изменение контактов: создание, правка, удаление и фото. */
interface ContactsWriter {
    /** Создаёт контакт в выбранном аккаунте. @return id нового контакта. */
    suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long

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
}
