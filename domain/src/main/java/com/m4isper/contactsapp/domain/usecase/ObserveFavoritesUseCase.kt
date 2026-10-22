package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoritesUseCase(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(): Flow<Set<Long>> = repository.observeFavorites()
}
