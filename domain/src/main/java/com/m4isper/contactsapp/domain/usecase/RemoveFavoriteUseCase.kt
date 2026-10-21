package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.Inject

@Inject
class RemoveFavoriteUseCase(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke(contactId: Long) = repository.removeFavorite(contactId)
}
