package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.repository.ContactsReader

/** Фото контакта для экспорта в .vcf; null, если фото нет. */
class LoadContactPhotoUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(contactId: Long): ByteArray? = repository.loadContactPhoto(contactId)
}
