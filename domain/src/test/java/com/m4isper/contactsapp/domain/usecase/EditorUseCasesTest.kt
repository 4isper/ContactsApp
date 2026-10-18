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

private class RecordingException(
    message: String,
) : RuntimeException(message)

private class RecordingRepository :
    ContactsRepository,
    FavoritesRepository {
    val created = mutableListOf<ContactForm>()
    val updated = mutableListOf<Pair<Long, ContactForm>>()
    val deleted = mutableListOf<Long>()
    val removedFavorites = mutableListOf<Long>()
    val failDeleteFor = mutableSetOf<Long>()
    val failFavoriteRemovalFor = mutableSetOf<Long>()

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
        if (contactId in failDeleteFor) throw RecordingException("delete failed for $contactId")
    }

    override fun observeFavorites(): Flow<Set<Long>> = flowOf(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) {
        removedFavorites += contactId
        if (contactId in failFavoriteRemovalFor) throw RecordingException("favorite cleanup failed for $contactId")
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

            val result = DeleteContactsUseCase(repository, repository)(setOf(7))

            assertEquals(DeleteContactsResult(deletedIds = setOf(7L), failedIds = emptySet()), result)
            assertEquals(listOf(7L), repository.deleted)
            assertEquals(listOf(7L), repository.removedFavorites)
        }

    @Test
    fun `delete keeps going after one failure and reports it`() =
        runTest {
            val repository = RecordingRepository().apply { failDeleteFor += 2L }
            val useCase = DeleteContactsUseCase(repository, repository)

            val result = useCase(setOf(1L, 2L, 3L))

            assertEquals(setOf(1L, 3L), result.deletedIds)
            assertEquals(setOf(2L), result.failedIds)
            assertEquals(listOf(1L, 2L, 3L), repository.deleted)
            assertEquals(listOf(1L, 3L), repository.removedFavorites)
        }

    @Test
    fun `failed favorite cleanup does not fail the deletion`() =
        runTest {
            val repository = RecordingRepository().apply { failFavoriteRemovalFor += 1L }
            val useCase = DeleteContactsUseCase(repository, repository)

            val result = useCase(setOf(1L))

            assertEquals(setOf(1L), result.deletedIds)
            assertTrue(result.allDeleted)
        }

    @Test
    fun `single delete delegates to batch and returns success`() =
        runTest {
            val repository = RecordingRepository()

            val deleted = DeleteContactUseCase(DeleteContactsUseCase(repository, repository))(7)

            assertTrue(deleted)
            assertEquals(listOf(7L), repository.deleted)
            assertEquals(listOf(7L), repository.removedFavorites)
        }
}
