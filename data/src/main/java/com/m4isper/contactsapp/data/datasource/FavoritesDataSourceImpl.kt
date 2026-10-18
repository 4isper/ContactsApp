package com.m4isper.contactsapp.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "favorites",
)

private val FAVORITE_IDS = stringSetPreferencesKey("favorite_contact_ids")

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FavoritesDataSourceImpl(
    private val context: Context,
) : FavoritesDataSource {
    override val favorites: Flow<Set<Long>> =
        context.favoritesDataStore.data
            .map { prefs -> prefs[FAVORITE_IDS] ?: emptySet() }
            .map { ids -> ids.mapNotNull { it.toLongOrNull() }.toSet() }

    override suspend fun toggle(contactId: Long) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[FAVORITE_IDS] ?: emptySet()
            val id = contactId.toString()
            prefs[FAVORITE_IDS] = if (id in current) current - id else current + id
        }
    }

    override suspend fun remove(contactId: Long) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[FAVORITE_IDS] ?: emptySet()
            prefs[FAVORITE_IDS] = current - contactId.toString()
        }
    }
}
