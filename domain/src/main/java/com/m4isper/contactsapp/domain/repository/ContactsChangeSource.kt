package com.m4isper.contactsapp.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Источник изменений провайдера контактов. Домен не знает про Android,
 * поэтому UI перечитывает список по каждому событию, а реализация живёт в data.
 */
interface ContactsChangeSource {
    fun observeChanges(): Flow<Unit>
}
