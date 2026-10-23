package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.repository.ContactsWriter

/** Записывает фото контакта готовыми байтами: используется при импорте .vcf. */
class SetContactPhotoUseCase(
    private val repository: ContactsWriter,
) {
    suspend operator fun invoke(
        contactId: Long,
        photo: ByteArray,
    ) = repository.setContactPhoto(contactId, photo)
}
