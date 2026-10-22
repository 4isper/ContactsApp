package com.m4isper.contactsapp.data.datasource

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import com.m4isper.contactsapp.domain.repository.ContactsChangeSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Наблюдает за ContactsContract и сообщает о создании, правке и удалении контактов. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContactsChangeSourceImpl(
    private val context: Context,
) : ContactsChangeSource {
    override fun observeChanges(): Flow<Unit> =
        callbackFlow {
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(
                        selfChange: Boolean,
                        uri: Uri?,
                    ) {
                        trySend(Unit)
                    }
                }
            val resolver = context.contentResolver
            OBSERVED_URIS.forEach { uri ->
                resolver.registerContentObserver(uri, true, observer)
            }
            awaitClose { resolver.unregisterContentObserver(observer) }
        }

    private companion object {
        /** Контакты, их data-строки и группы покрывают создание, правку, удаление и членство. */
        val OBSERVED_URIS =
            listOf(
                ContactsContract.Contacts.CONTENT_URI,
                ContactsContract.Data.CONTENT_URI,
                ContactsContract.Groups.CONTENT_URI,
            )
    }
}
