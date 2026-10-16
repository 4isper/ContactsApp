package com.m4isper.contactsapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(): Flow<Set<Long>>

    suspend fun toggleFavorite(contactId: Long)

    suspend fun removeFavorite(contactId: Long)
}
