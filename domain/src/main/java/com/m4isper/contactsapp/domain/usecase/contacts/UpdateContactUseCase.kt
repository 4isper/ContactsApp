package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.normalized
import com.m4isper.contactsapp.domain.model.contact.validateContactForm
import com.m4isper.contactsapp.domain.repository.ContactsWriter

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
