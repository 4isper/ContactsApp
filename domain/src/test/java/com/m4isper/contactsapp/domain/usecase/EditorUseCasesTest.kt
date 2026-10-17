package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingRepository :
    ContactsRepository,
    FavoritesRepository {
    val created = mutableListOf<ContactForm>()
    val updated = mutableListOf<Pair<Long, ContactForm>>()
    val deleted = mutableListOf<Long>()
    val removedFavorites = mutableListOf<Long>()

    override suspend fun getContacts(): List<ContactDetails> = emptyList()

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun createContact(form: ContactForm) {
        created += form
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        updated += contactId to form
    }

    override suspend fun deleteContact(contactId: Long) {
        deleted += contactId
    }

    override fun observeFavorites(): Flow<Set<Long>> = flowOf(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) {
        removedFavorites += contactId
    }
}

class EditorUseCasesTest {
    @Test
    fun `create saves valid form`() =
        runTest {
            val repository = RecordingRepository()
            val form =
                ContactForm(
                    name = EditableName(display = "Anna"),
                    phones = listOf(EditablePhone(number = "111")),
                )

            val result = CreateContactUseCase(repository)(form)

            assertEquals(SaveResult.Saved, result)
            assertEquals(listOf(form), repository.created)
        }

    @Test
    fun `create rejects invalid form without touching repository`() =
        runTest {
            val repository = RecordingRepository()

            val result = CreateContactUseCase(repository)(ContactForm(name = EditableName(display = "  ")))

            assertEquals(SaveResult.Invalid(FormError.EMPTY_FORM), result)
            assertTrue(repository.created.isEmpty())
        }

    @Test
    fun `update saves valid form`() =
        runTest {
            val repository = RecordingRepository()
            val form = ContactForm(name = EditableName(display = "Bob"))

            val result = UpdateContactUseCase(repository)(7, form)

            assertEquals(SaveResult.Saved, result)
            assertEquals(listOf(7L to form), repository.updated)
        }

    @Test
    fun `update rejects invalid form without touching repository`() =
        runTest {
            val repository = RecordingRepository()

            val result =
                UpdateContactUseCase(repository)(
                    7,
                    ContactForm(
                        name = EditableName(display = "Bob"),
                        phones = listOf(EditablePhone(number = "")),
                    ),
                )

            assertEquals(SaveResult.Invalid(FormError.BLANK_PHONE), result)
            assertTrue(repository.updated.isEmpty())
        }

    @Test
    fun `delete removes contact and clears favorite`() =
        runTest {
            val repository = RecordingRepository()

            DeleteContactUseCase(repository, repository)(7)

            assertEquals(listOf(7L), repository.deleted)
            assertEquals(listOf(7L), repository.removedFavorites)
        }
}
