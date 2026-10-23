package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun details(
    id: Long,
    name: String,
    vararg phones: String,
) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
)

private class StubContactsRepository(
    private val contacts: List<ContactDetails>,
) : ContactsReader {
    override suspend fun getContacts(): List<ContactDetails> = contacts

    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = emptyList()

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun getContactDetails(contactId: Long): ContactDetails? =
        contacts.firstOrNull { it.contactId == contactId }
}

class GetContactsUseCaseTest {
    @Test
    fun `invoke returns contacts from repository`() =
        runTest {
            val expected =
                listOf(
                    details(1, "Anna", "+100"),
                    details(2, "Bob", "+200"),
                )
            val useCase = GetContactsUseCase(StubContactsRepository(expected))

            assertEquals(expected, useCase())
        }

    @Test
    fun `invoke returns empty list when repository is empty`() =
        runTest {
            val useCase = GetContactsUseCase(StubContactsRepository(emptyList()))

            assertEquals(emptyList<ContactDetails>(), useCase())
        }
}

class GetContactDetailsUseCaseTest {
    @Test
    fun `invoke returns contact by id`() =
        runTest {
            val expected = details(7, "Anna", "+100", "+101")
            val repository =
                StubContactsRepository(
                    listOf(expected, details(8, "Bob", "+200")),
                )
            val useCase = GetContactDetailsUseCase(repository)

            assertEquals(expected, useCase(7))
        }

    @Test
    fun `invoke returns null for unknown id`() =
        runTest {
            val useCase = GetContactDetailsUseCase(StubContactsRepository(emptyList()))

            assertNull(useCase(42))
        }
}
