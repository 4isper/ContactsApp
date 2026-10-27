package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.usecase.contacts.CopyContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.MoveContactToAccountUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Копирование и перенос контакта между аккаунтами — правила предметной области. */
class CopyContactToAccountUseCaseTest {
    private class FakeReader(
        private val form: ContactForm?,
        private val photo: ByteArray? = null,
    ) : ContactsReader {
        override suspend fun getContacts(): List<ContactDetails> = emptyList()

        override suspend fun getContactsPage(
            offset: Int,
            limit: Int,
        ): List<ContactDetails> = emptyList()

        override suspend fun getContactDetails(contactId: Long): ContactDetails? = null

        override suspend fun getContactForm(contactId: Long): ContactForm? = form

        override suspend fun loadContactPhoto(contactId: Long): ByteArray? = photo

        override suspend fun getProfileContactId(): Long? = null
    }

    private class FakeWriter : ContactsWriter {
        val created = mutableListOf<ContactForm>()
        val createdAccounts = mutableListOf<ContactAccount>()
        val photos = mutableListOf<Pair<Long, ByteArray>>()
        val deleted = mutableListOf<Long>()

        override suspend fun createContact(
            form: ContactForm,
            account: ContactAccount,
        ): Long {
            created += form
            createdAccounts += account
            return 99L
        }

        override suspend fun updateContact(
            contactId: Long,
            form: ContactForm,
        ) = Unit

        override suspend fun deleteContact(contactId: Long) {
            deleted += contactId
        }

        override suspend fun setContactPhoto(
            contactId: Long,
            photo: ByteArray,
        ) {
            photos += contactId to photo
        }
    }

    private val google = ContactAccount(name = "user@gmail.com", type = "com.google")

    @Test
    fun `copy creates the contact in the target account and keeps the source`() =
        runTest {
            val writer = FakeWriter()
            val form = ContactForm()
            val useCase = CopyContactToAccountUseCase(FakeReader(form), writer)

            val newId = useCase(contactId = 7, account = google)

            assertEquals(listOf(form), writer.created)
            assertEquals(listOf(google), writer.createdAccounts)
            assertEquals(99L, newId)
            assertTrue(writer.deleted.isEmpty())
        }

    @Test
    fun `copy carries the photo of the source`() =
        runTest {
            val writer = FakeWriter()
            val photo = byteArrayOf(1, 2, 3)
            val useCase = CopyContactToAccountUseCase(FakeReader(ContactForm(), photo), writer)

            useCase(contactId = 7, account = google)

            assertEquals(listOf(99L to photo), writer.photos)
        }

    @Test
    fun `copy without a form fails`() =
        runTest {
            val useCase = CopyContactToAccountUseCase(FakeReader(null), FakeWriter())

            val failure = runCatching { useCase(contactId = 7, account = google) }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
        }

    @Test
    fun `move creates the copy and deletes the source`() =
        runTest {
            val writer = FakeWriter()
            val copy = CopyContactToAccountUseCase(FakeReader(ContactForm()), writer)
            val useCase = MoveContactToAccountUseCase(copy, writer)

            useCase(contactId = 7, account = google)

            assertEquals(listOf(google), writer.createdAccounts)
            assertEquals(listOf(7L), writer.deleted)
        }
}
