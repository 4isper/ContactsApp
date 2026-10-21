package com.m4isper.contactsapp.data.datasource

import kotlinx.coroutines.flow.Flow

/** Любимые контакты: поток id и операции изменения. */
interface FavoritesDataSource {
    val favorites: Flow<Set<Long>>

    suspend fun toggle(contactId: Long)

    suspend fun remove(contactId: Long)

    /** Перечитать избранное: нужно после выдачи разрешения на контакты. */
    suspend fun refresh()
}
