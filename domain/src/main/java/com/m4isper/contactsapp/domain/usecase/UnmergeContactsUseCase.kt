package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactLink
import com.m4isper.contactsapp.domain.repository.ContactsMerger

/** Отменяет слияние: связанные контакты снова видны отдельно. */
class UnmergeContactsUseCase(
    private val contactsRepository: ContactsMerger,
) {
    suspend operator fun invoke(link: ContactLink) = contactsRepository.unmergeContacts(link)
}
