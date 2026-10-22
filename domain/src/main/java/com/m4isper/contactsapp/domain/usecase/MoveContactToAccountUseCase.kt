package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import dev.zacsweers.metro.Inject

/**
 * Переносит контакт в другой аккаунт: создаёт копию и удаляет исходный.
 * Порядок важный: копия готова до удаления, иначе контакт может потеряться.
 */
@Inject
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
