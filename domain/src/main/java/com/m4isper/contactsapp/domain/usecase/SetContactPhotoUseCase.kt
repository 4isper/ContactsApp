package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

/** Записывает фото контакта готовыми байтами: используется при импорте .vcf. */
@Inject
class SetContactPhotoUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(
        contactId: Long,
        photo: ByteArray,
    ) = repository.setContactPhoto(contactId, photo)
}
