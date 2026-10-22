package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactLink
import com.m4isper.contactsapp.domain.repository.ContactsMerger

/**
 * Слияние дубликатов: источники связываются с [targetId] и становятся его частью.
 * Связь возвращается наверх, чтобы слияние можно было отменить.
 */
class MergeContactsUseCase(
    private val contactsRepository: ContactsMerger,
) {
    suspend operator fun invoke(
        targetId: Long,
        sourceIds: List<Long>,
    ): ContactLink? {
        val sources = sourceIds.filterNot { it == targetId }
        if (sources.isEmpty()) return null
        return contactsRepository.mergeContacts(targetId, sources)
    }
}
