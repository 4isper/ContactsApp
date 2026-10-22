package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactLink
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import dev.zacsweers.metro.Inject

/** Отменяет слияние: связанные контакты снова видны отдельно. */
@Inject
class UnmergeContactsUseCase(
    private val contactsRepository: ContactsMerger,
) {
    suspend operator fun invoke(link: ContactLink) = contactsRepository.unmergeContacts(link)
}
