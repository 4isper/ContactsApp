package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class ImportFakeRepository(
    private val failAt: Int? = null,
) : ContactsRepository {
    val created = mutableListOf<ContactForm>()
    private var attempts = 0

    override suspend fun getContacts(): List<ContactDetails> = emptyList()

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun createContact(form: ContactForm) {
        val index = attempts
        attempts += 1
        if (failAt == index) error("import failed")
        created += form
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit
}

private fun form(name: String) = ContactForm(name = EditableName(display = name))

class ImportContactsUseCaseTest {
    @Test
    fun `creates every parsed contact`() =
        runTest {
            val repository = ImportFakeRepository()
            val forms = listOf(form("Anna"), form("Bob"))

            val imported = ImportContactsUseCase(repository)(forms)

            assertEquals(2, imported)
            assertEquals(forms, repository.created)
        }

    @Test
    fun `skips failing contact and keeps importing`() =
        runTest {
            val repository = ImportFakeRepository(failAt = 1)
            val forms = listOf(form("Anna"), form("Bob"), form("Cara"))

            val imported = ImportContactsUseCase(repository)(forms)

            assertEquals(2, imported)
            assertEquals(listOf("Anna", "Cara"), repository.created.map { it.name.display })
        }

    @Test
    fun `empty list reports zero`() =
        runTest {
            val imported = ImportContactsUseCase(ImportFakeRepository())(emptyList())

            assertEquals(0, imported)
        }
}
