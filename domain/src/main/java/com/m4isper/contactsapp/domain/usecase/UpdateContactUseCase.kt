package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.normalized
import com.m4isper.contactsapp.domain.model.validateContactForm
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import dev.zacsweers.metro.Inject

@Inject
class UpdateContactUseCase(
    private val repository: ContactsWriter,
) {
    suspend operator fun invoke(
        contactId: Long,
        form: ContactForm,
    ): SaveResult {
        val normalized = form.normalized()
        validateContactForm(normalized)?.let { return SaveResult.Invalid(it) }
        repository.updateContact(contactId, normalized)
        return SaveResult.Saved(contactId)
    }
}
