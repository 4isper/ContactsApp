package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.repository.ContactsChangeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Фейк источника изменений провайдера: тест сам решает, когда «пришло» событие. */
internal class FakeContactsChangeSource : ContactsChangeSource {
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeChanges(): Flow<Unit> = changes

    fun notifyChange() {
        changes.tryEmit(Unit)
    }
}
