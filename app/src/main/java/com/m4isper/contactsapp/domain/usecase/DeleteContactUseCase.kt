package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.Inject

@Inject
class DeleteContactUseCase(
    private val repository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository,
) {
    suspend operator fun invoke(contactId: Long) {
        repository.deleteContact(contactId)
        favoritesRepository.removeFavorite(contactId)
    }
}
