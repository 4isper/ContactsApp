package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
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
    ContactsReader,
    ContactsAccounts,
    ContactsWriter,
    ContactsMerger,
    FavoritesRepository {
    val created = mutableListOf<ContactForm>()

    override suspend fun unmergeContacts(link: com.m4isper.contactsapp.domain.model.ContactLink) = Unit

    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = emptyList()

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun getContactAccount(contactId: Long): ContactAccount = ContactAccount.Local

    override suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

    val createdAccounts = mutableListOf<ContactAccount>()
    val updated = mutableListOf<Pair<Long, ContactForm>>()
    val deleted = mutableListOf<Long>()
    val merged = mutableListOf<Pair<Long, List<Long>>>()
    val removedFavorites = mutableListOf<Long>()
    val failDeleteFor = mutableSetOf<Long>()
    val failFavoriteRemovalFor = mutableSetOf<Long>()

    override suspend fun getContacts(): List<ContactDetails> = emptyList()

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long {
        created += form
        createdAccounts += account
        return created.size.toLong()
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        updated += contactId to form
    }

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) {
        deleted += contactId
        if (contactId in failDeleteFor) throw RecordingException("delete failed for $contactId")
    }

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.ContactLink? {
        merged += targetId to sourceIds
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

            assertEquals(SaveResult.Saved(1L), result)
            assertEquals(listOf(form), repository.created)
            assertEquals(listOf(ContactAccount.Local), repository.createdAccounts)
        }

    @Test
    fun `create normalizes blank rows and labels before saving`() =
        runTest {
            val repository = RecordingRepository()
            val form =
                ContactForm(
                    name = EditableName(display = "Anna"),
                    phones =
                        listOf(
                            EditablePhone(number = "111", customLabel = "  "),
                            EditablePhone(number = "   "),
                        ),
                )

            val result = CreateContactUseCase(repository)(form)

            assertEquals(SaveResult.Saved(1L), result)
            assertEquals(listOf(EditablePhone(number = "111", customLabel = null)), repository.created.single().phones)
        }

    @Test
    fun `create saves to the selected account`() =
        runTest {
            val repository = RecordingRepository()
            val form = ContactForm(name = EditableName(display = "Anna"))
            val account = ContactAccount(name = "user@gmail.com", type = "com.google")

            val result = CreateContactUseCase(repository)(form, account)

            assertEquals(SaveResult.Saved(1L), result)
            assertEquals(listOf(account), repository.createdAccounts)
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

            assertEquals(SaveResult.Saved(7L), result)
            assertEquals(listOf(7L to form), repository.updated)
        }

    @Test
    fun `update rejects empty form without touching repository`() =
        runTest {
            val repository = RecordingRepository()

            val result = UpdateContactUseCase(repository)(7, ContactForm())

            assertEquals(SaveResult.Invalid(FormError.EMPTY_FORM), result)
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
}
