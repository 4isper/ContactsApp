package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.model.normalized
import com.m4isper.contactsapp.domain.model.validateContactForm
import com.m4isper.contactsapp.domain.repository.ContactsWriter

sealed interface SaveResult {
    /** Контакт сохранён; [contactId] — id созданного контакта. */
    data class Saved(
        val contactId: Long,
    ) : SaveResult

    data class Invalid(
        val error: FormError,
    ) : SaveResult
}

class CreateContactUseCase(
    private val repository: ContactsWriter,
) {
    suspend operator fun invoke(
        form: ContactForm,
        account: ContactAccount = ContactAccount.Local,
    ): SaveResult {
        val normalized = form.normalized()
        validateContactForm(normalized)?.let { return SaveResult.Invalid(it) }
        val contactId = repository.createContact(normalized, account)
        return SaveResult.Saved(contactId)
    }
}
