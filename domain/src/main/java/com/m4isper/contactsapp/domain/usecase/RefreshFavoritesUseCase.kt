package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.Inject

/** Перечитывает избранное из провайдера: нужно после выдачи разрешения на контакты. */
@Inject
class RefreshFavoritesUseCase(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke() = repository.refreshFavorites()
}
