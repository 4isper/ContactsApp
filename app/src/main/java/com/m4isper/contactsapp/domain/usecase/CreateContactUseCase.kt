package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.model.validateContactForm
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

sealed interface SaveResult {
    data object Saved : SaveResult

    data class Invalid(
        val error: FormError,
    ) : SaveResult
}

@Inject
class CreateContactUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(form: ContactForm): SaveResult {
        validateContactForm(form)?.let { return SaveResult.Invalid(it) }
        repository.createContact(form)
        return SaveResult.Saved
    }
}
