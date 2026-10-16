package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class ObserveFavoritesUseCase(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(): Flow<Set<Long>> = repository.observeFavorites()
}
