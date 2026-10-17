package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.Inject

@Inject
class DeleteContactsUseCase(
    private val repository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository,
) {
    suspend operator fun invoke(contactIds: Set<Long>) {
        contactIds.forEach { contactId ->
            repository.deleteContact(contactId)
            favoritesRepository.removeFavorite(contactId)
        }
    }
}
