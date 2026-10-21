package com.m4isper.contactsapp.data.datasource

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "favorites",
)

/** Прежнее хранилище избранного: нужно только для разовой миграции в провайдер. */
private val LEGACY_FAVORITE_IDS = stringSetPreferencesKey("favorite_contact_ids")

/** Тип data-строки «звезда»: провайдер синхронизирует её вместе с контактом. */
private const val STARRED_MIME_TYPE = "vnd.android.cursor.item/starred"

/**
 * Избранное хранится в провайдере ([ContactsContract] starred-строки), а не в настройках
 * приложения: так звезда синхронизируется с системными контактами и переживает
 * удаление контакта. Прежнее хранилище переносится в провайдер один раз.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FavoritesDataSourceImpl(
    private val context: Context,
) : FavoritesDataSource {
    /** Перечитывание по требованию: нужно после выдачи разрешения на контакты. */
    private val refreshTrigger = MutableStateFlow(0L)

    override val favorites: Flow<Set<Long>> =
        callbackFlow {
            val resolver = context.contentResolver
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(
                        selfChange: Boolean,
                        uri: Uri?,
                    ) {
                        trySend(loadStarred())
                    }
                }
            // Наблюдатель провайдера требует разрешения на контакты: без него не подключаемся.
            var observing = false

            fun startObserving() {
                if (!observing && canReadContacts()) {
                    resolver.registerContentObserver(ContactsContract.Data.CONTENT_URI, true, observer)
                    observing = true
                }
            }

            fun stopObserving() {
                if (observing) {
                    resolver.unregisterContentObserver(observer)
                    observing = false
                }
            }
            startObserving()
            if (canReadContacts()) migrateLegacyFavorites()
            trySend(loadStarred())
            launch {
                // Разрешение могло выдать только что: перечитываем и включаем наблюдение.
                refreshTrigger.collect {
                    startObserving()
                    if (canReadContacts()) migrateLegacyFavorites()
                    trySend(loadStarred())
                }
            }
            awaitClose { stopObserving() }
        }.distinctUntilChanged()

    override suspend fun toggle(contactId: Long) {
        val rawIds = rawContactIds(contactId)
        if (rawIds.isEmpty()) return
        if (rawIds.any { isStarred(it) }) {
            rawIds.forEach(::removeStarred)
        } else {
            rawIds.forEach(::addStarred)
        }
    }

    override suspend fun remove(contactId: Long) {
        rawContactIds(contactId).forEach(::removeStarred)
    }

    override suspend fun refresh() {
        refreshTrigger.value += 1
    }

    /** Звёзды агрегированного контакта: id его raw-контактов. */
    private fun rawContactIds(contactId: Long): List<Long> {
        val cursor =
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID}=?",
                arrayOf(contactId.toString()),
                null,
            )
        return cursor
            ?.use {
                if (!it.moveToFirst()) {
                    emptyList()
                } else {
                    val idIndex = it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
                    buildList {
                        do {
                            add(it.getLong(idIndex))
                        } while (it.moveToNext())
                    }
                }
            }.orEmpty()
    }

    /** Id агрегированных контактов, у которых есть starred-строка. */
    private fun loadStarred(): Set<Long> {
        if (!canReadContacts()) return emptySet()
        val cursor =
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.CONTACT_ID),
                "${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(STARRED_MIME_TYPE),
                null,
            )
        return cursor
            ?.use {
                if (!it.moveToFirst()) {
                    emptySet()
                } else {
                    val idIndex = it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                    buildSet {
                        do {
                            add(it.getLong(idIndex))
                        } while (it.moveToNext())
                    }
                }
            }.orEmpty()
    }

    private fun isStarred(rawContactId: Long): Boolean {
        val cursor =
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(rawContactId.toString(), STARRED_MIME_TYPE),
                null,
            )
        return cursor?.use { it.moveToFirst() } == true
    }

    private fun addStarred(rawContactId: Long) {
        val values =
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, STARRED_MIME_TYPE)
            }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)
    }

    private fun removeStarred(rawContactId: Long) {
        context.contentResolver.delete(
            ContactsContract.Data.CONTENT_URI,
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(rawContactId.toString(), STARRED_MIME_TYPE),
        )
    }

    /**
     * Разовая миграция прежнего списка избранного в провайдер: id, которых больше нет
     * в контактах, просто отбрасываются. Старое хранилище очищается.
     */
    private suspend fun migrateLegacyFavorites() {
        if (!canReadContacts()) return
        val legacy =
            context.favoritesDataStore.data
                .first()[LEGACY_FAVORITE_IDS]
                .orEmpty()
        if (legacy.isEmpty()) return
        legacy.mapNotNull { it.toLongOrNull() }.forEach { contactId ->
            if (!isStarredContact(contactId)) {
                rawContactIds(contactId).forEach(::addStarred)
            }
        }
        context.favoritesDataStore.edit { prefs ->
            prefs[LEGACY_FAVORITE_IDS] = emptySet()
        }
    }

    private fun isStarredContact(contactId: Long): Boolean =
        context.contentResolver
            .query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(contactId.toString(), STARRED_MIME_TYPE),
                null,
            )?.use { it.moveToFirst() } == true

    /** Без разрешения на контакты провайдер закрыт: избранное считаем пустым. */
    private fun canReadContacts(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
}
