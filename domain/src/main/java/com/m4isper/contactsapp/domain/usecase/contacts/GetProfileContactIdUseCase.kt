package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.repository.ContactsReader

/** id контакта «Мой профиль»; null, если профиля нет. */
class GetProfileContactIdUseCase(
    private val repository: ContactsReader,
) {
    suspend operator fun invoke(): Long? = repository.getProfileContactId()
}
