package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.FavoritesDataSource
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FavoritesRepositoryImpl(
    private val dataSource: FavoritesDataSource,
) : FavoritesRepository {
    override fun observeFavorites(): Flow<Set<Long>> = dataSource.favorites

    override suspend fun toggleFavorite(contactId: Long) = dataSource.toggle(contactId)
}
