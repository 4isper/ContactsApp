package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

/** Фото контакта для экспорта в .vcf; null, если фото нет. */
@Inject
class LoadContactPhotoUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(contactId: Long): ByteArray? = repository.loadContactPhoto(contactId)
}
