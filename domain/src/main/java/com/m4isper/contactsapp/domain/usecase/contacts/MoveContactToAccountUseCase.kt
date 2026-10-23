package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsWriter

/**
 * Переносит контакт в другой аккаунт: создаёт копию и удаляет исходный.
 * Порядок важный: копия готова до удаления, иначе контакт может потеряться.
 */
class MoveContactToAccountUseCase(
    private val copyContact: CopyContactToAccountUseCase,
    private val writer: ContactsWriter,
) {
    suspend operator fun invoke(
        contactId: Long,
        account: ContactAccount,
    ): Long {
        val newId = copyContact(contactId, account)
        writer.deleteContact(contactId)
        return newId
    }
}
