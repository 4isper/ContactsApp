package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * Слияние дубликатов: данные контактов [sourceIds] переезжают в [targetId],
 * источники удаляются. Избранное не теряется: если источник был избранным,
 * избранным становится целевой контакт.
 */
@Inject
class MergeContactsUseCase(
    private val contactsRepository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository,
) {
    suspend operator fun invoke(
        targetId: Long,
        sourceIds: List<Long>,
    ) {
        val sources = sourceIds.filterNot { it == targetId }
        if (sources.isEmpty()) return
        contactsRepository.mergeContacts(targetId, sources)
        val favorites = favoritesRepository.observeFavorites().first()
        if (targetId !in favorites && sources.any { it in favorites }) {
            favoritesRepository.toggleFavorite(targetId)
        }
        sources.forEach { favoritesRepository.removeFavorite(it) }
    }
}
