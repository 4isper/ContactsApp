package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException

/** Результат пакетного удаления: [deletedIds] — удалено, [failedIds] — не удалось удалить. */
data class DeleteContactsResult(
    val deletedIds: Set<Long>,
    val failedIds: Set<Long>,
) {
    val allDeleted: Boolean
        get() = failedIds.isEmpty()
}

@Inject
class DeleteContactsUseCase(
    private val repository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository,
) {
    /**
     * Удаляет контакты по одному: сбой на одном не отменяет остальные, чтобы частичный
     * сбой можно было показать честно, а не атомарно откатить (провайдер не даёт транзакций).
     */
    @Suppress("TooGenericExceptionCaught") // Осознанно: проблемный контакт не должен ронять оставшиеся.
    suspend operator fun invoke(contactIds: Set<Long>): DeleteContactsResult {
        val deleted = mutableSetOf<Long>()
        val failed = mutableSetOf<Long>()
        val favoritesToCleanup = mutableSetOf<Long>()
        contactIds.forEach { contactId ->
            try {
                repository.deleteContact(contactId)
                deleted += contactId
                favoritesToCleanup += contactId
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed += contactId
            }
        }
        cleanupFavorites(favoritesToCleanup)
        return DeleteContactsResult(deletedIds = deleted, failedIds = failed)
    }

    /** Избранное чистится отдельно: сбой чистки не отменяет уже выполненное удаление. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой чистки избранного не важен для результата удаления.
    private suspend fun cleanupFavorites(contactIds: Set<Long>) {
        contactIds.forEach { contactId ->
            try {
                favoritesRepository.removeFavorite(contactId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Контакт уже удалён; осиротевшая запись избранного безвредна.
            }
        }
    }
}
