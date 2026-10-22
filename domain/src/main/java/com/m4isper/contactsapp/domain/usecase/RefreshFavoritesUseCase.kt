package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.FavoritesRepository

/** Перечитывает избранное из провайдера: нужно после выдачи разрешения на контакты. */
class RefreshFavoritesUseCase(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke() = repository.refreshFavorites()
}
