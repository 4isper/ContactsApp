package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

/** id контакта «Мой профиль»; null, если профиля нет. */
@Inject
class GetProfileContactIdUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(): Long? = repository.getProfileContactId()
}
