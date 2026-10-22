package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.FavoritesRepository

class RemoveFavoriteUseCase(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke(contactId: Long) = repository.removeFavorite(contactId)
}
