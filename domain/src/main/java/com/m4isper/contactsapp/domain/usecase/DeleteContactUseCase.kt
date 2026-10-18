package com.m4isper.contactsapp.domain.usecase

import dev.zacsweers.metro.Inject

@Inject
class DeleteContactUseCase(
    private val deleteContacts: DeleteContactsUseCase,
) {
    /** @return true, если контакт удалён вместе с записью избранного. */
    suspend operator fun invoke(contactId: Long): Boolean = deleteContacts(setOf(contactId)).allDeleted
}
