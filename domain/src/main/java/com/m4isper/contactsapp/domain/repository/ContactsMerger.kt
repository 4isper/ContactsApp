package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactLink

/** Слияние контактов: источники связываются с целевым и становятся его частью. */
interface ContactsMerger {
    /**
     * Слияние дубликатов: источники связываются с целевым и становятся его частью.
     * @return связь для отмены слияния.
     */
    suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): ContactLink?

    /** Отменяет слияние: контакты снова видны отдельно. */
    suspend fun unmergeContacts(link: ContactLink)
}
