package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.FavoritesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesRepositoryImplTest {
    private class FakeDs : FavoritesDataSource {
        val flow = MutableStateFlow<Set<Long>>(emptySet())
        val toggled = mutableListOf<Long>()
        val removed = mutableListOf<Long>()
        var refreshed = 0
        override val favorites: Flow<Set<Long>> = flow

        override suspend fun toggle(contactId: Long) {
            toggled += contactId
            flow.value = flow.value + contactId
        }

        override suspend fun remove(contactId: Long) {
            removed += contactId
            flow.value = flow.value - contactId
        }

        override suspend fun refresh() {
            refreshed++
        }
    }

    @Test
    fun observeFavoritesDelegates() =
        runBlocking {
            val ds = FakeDs()
            val repo = FavoritesRepositoryImpl(ds)
            ds.flow.value = setOf(1L, 2L)
            assertEquals(setOf(1L, 2L), repo.observeFavorites().first())
        }

    @Test
    fun toggleDelegates() =
        runBlocking {
            val ds = FakeDs()
            val repo = FavoritesRepositoryImpl(ds)
            repo.toggleFavorite(5L)
            assertEquals(listOf(5L), ds.toggled)
        }

    @Test
    fun removeDelegates() =
        runBlocking {
            val ds = FakeDs()
            val repo = FavoritesRepositoryImpl(ds)
            repo.removeFavorite(5L)
            assertEquals(listOf(5L), ds.removed)
        }

    @Test
    fun refreshDelegates() =
        runBlocking {
            val ds = FakeDs()
            val repo = FavoritesRepositoryImpl(ds)
            repo.refreshFavorites()
            assertEquals(1, ds.refreshed)
        }
}
