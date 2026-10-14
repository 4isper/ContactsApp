package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.Contact
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class StubContactsRepository(
    private val contacts: List<Contact>,
) : ContactsRepository {
    override suspend fun getContacts(): List<Contact> = contacts
}

class GetContactsUseCaseTest {

    @Test
    fun `invoke returns contacts from repository`() = runTest {
        val expected = listOf(
            Contact(id = 1, name = "Anna", phone = "+100"),
            Contact(id = 2, name = "Bob", phone = "+200"),
        )
        val useCase = GetContactsUseCase(StubContactsRepository(expected))

        assertEquals(expected, useCase())
    }

    @Test
    fun `invoke returns empty list when repository is empty`() = runTest {
        val useCase = GetContactsUseCase(StubContactsRepository(emptyList()))

        assertEquals(emptyList<Contact>(), useCase())
    }
}
