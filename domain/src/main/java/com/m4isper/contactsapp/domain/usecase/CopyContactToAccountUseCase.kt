package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import dev.zacsweers.metro.Inject

/**
 * Копирует контакт в другой аккаунт: исходный остаётся на месте.
 * Логика живёт здесь, а не в порте: порт описывает контракт и ничего не решает.
 */
@Inject
class CopyContactToAccountUseCase(
    private val reader: ContactsReader,
    private val writer: ContactsWriter,
) {
    suspend operator fun invoke(
        contactId: Long,
        account: ContactAccount,
    ): Long {
        val form = reader.getContactForm(contactId) ?: error("no contact form for id=$contactId")
        val newId = writer.createContact(form, account)
        reader.loadContactPhoto(contactId)?.let { photo -> writer.setContactPhoto(newId, photo) }
        return newId
    }
}
