package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.validateContactForm
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject

@Inject
class UpdateContactUseCase(
    private val repository: ContactsRepository,
) {
    suspend operator fun invoke(
        contactId: Long,
        form: ContactForm,
    ): SaveResult {
        validateContactForm(form)?.let { return SaveResult.Invalid(it) }
        repository.updateContact(contactId, form)
        return SaveResult.Saved
    }
}
