package com.m4isper.contactsapp.testing

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactLink
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Общие заготовки для тестов: контакт, репозиторий и сбой удаления.
 * Их используют тесты экранов, чтобы не собирать одни и те же фейки заново.
 */
internal fun testDetails(
    id: Long,
    name: String,
    vararg phones: String,
) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
)

/** Технический сбой удаления одного контакта, независимый от [FakeContactsRepository.failure]. */
internal class DeleteFailedException : RuntimeException("delete failed")

internal class FakeContactsRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var failure: Throwable? = null,
    initialFavorites: Set<Long> = emptySet(),
    var accounts: List<ContactAccount> = listOf(ContactAccount.Local),
) : ContactsReader,
    ContactsAccounts,
    ContactsWriter,
    ContactsMerger,
    FavoritesRepository {
    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun getContactAccount(contactId: Long): ContactAccount = ContactAccount.Local

    override suspend fun unmergeContacts(link: ContactLink) = Unit

    private val favoritesFlow = MutableStateFlow(initialFavorites)
    val deleted = mutableListOf<Long>()
    val created = mutableListOf<ContactForm>()

    /** Удаление этих контактов падает технически (в отличие от [failure], не влияет на загрузку). */
    val failDeleteFor = mutableSetOf<Long>()

    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> {
        failure?.let { throw it }
        return contacts.drop(offset).take(limit)
    }

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
        failure?.let { throw it }
        return contacts.firstOrNull { it.contactId == contactId }
    }

    override suspend fun getContactAccounts(): List<ContactAccount> {
        failure?.let { throw it }
        return accounts
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

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long {
        failure?.let { throw it }
        created += form
        val newId = (contacts.maxOfOrNull { it.contactId } ?: 0L) + 1
        val name = form.name.display.ifBlank { form.name.composed }
        contacts = contacts + ContactDetails(contactId = newId, name = name)
        return newId
    }

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) {
        failure?.let { throw it }
        if (contactId in failDeleteFor) throw DeleteFailedException()
        deleted += contactId
        contacts = contacts.filterNot { it.contactId == contactId }
    }

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.ContactLink? {
        failure?.let { throw it }
        contacts = contacts.filterNot { it.contactId in sourceIds }
        return com.m4isper.contactsapp.domain.model.ContactLink(
            target =
                com.m4isper.contactsapp.domain.model
                    .RawContactRef(targetId),
            sources =
                sourceIds.map {
                    com.m4isper.contactsapp.domain.model
                        .RawContactRef(it)
                },
        )
    }
}
