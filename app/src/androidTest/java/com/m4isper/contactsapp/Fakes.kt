package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun testDetails(
    id: Long,
    name: String,
    vararg phones: String,
) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
)

internal class FakeAndroidRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var failure: Throwable? = null,
    initialFavorites: Set<Long> = emptySet(),
) : ContactsRepository,
    FavoritesRepository {
    private val favoritesFlow = MutableStateFlow(initialFavorites)
    val created = mutableListOf<ContactForm>()
    val updated = mutableListOf<Pair<Long, ContactForm>>()
    val deleted = mutableListOf<Long>()

    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

    override suspend fun getContactForm(contactId: Long): ContactForm? {
        failure?.let { throw it }
        val details = contacts.firstOrNull { it.contactId == contactId } ?: return null
        return ContactForm(
            name = details.name,
            phones = details.phones.map { EditablePhone(number = it.number, kind = it.kind) },
        )
    }

    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
        failure?.let { throw it }
        return contacts.firstOrNull { it.contactId == contactId }
    }

    override suspend fun createContact(form: ContactForm) {
        failure?.let { throw it }
        created += form
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        failure?.let { throw it }
        updated += contactId to form
    }

    override suspend fun deleteContact(contactId: Long) {
        failure?.let { throw it }
        deleted += contactId
    }

    override fun observeFavorites(): Flow<Set<Long>> = favoritesFlow.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) {
        val current = favoritesFlow.value
        favoritesFlow.value =
            if (contactId in current) {
                current - contactId
            } else {
                current + contactId
            }
    }

    override suspend fun removeFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value - contactId
    }
}
