package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactLink
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class MergeFakeRepository(
    initialFavorites: Set<Long> = emptySet(),
    private val failMerge: Boolean = false,
) : ContactsRepository,
    FavoritesRepository {
    val merged = mutableListOf<Pair<Long, List<Long>>>()
    val unmerged = mutableListOf<ContactLink>()
    val favoritesFlow = MutableStateFlow(initialFavorites)

    override suspend fun getContacts(): List<ContactDetails> = emptyList()

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun createContact(form: ContactForm): Long = 1L

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): ContactLink? {
        if (failMerge) error("merge failed")
        merged += targetId to sourceIds
        return ContactLink(targetRawContactId = targetId, sourceRawContactIds = sourceIds)
    }

    override suspend fun unmergeContacts(link: ContactLink) {
        unmerged += link
    }

    override suspend fun copyContact(
        contactId: Long,
        account: com.m4isper.contactsapp.domain.model.ContactAccount,
    ): Long = 2L

    override suspend fun moveContact(
        contactId: Long,
        account: com.m4isper.contactsapp.domain.model.ContactAccount,
    ): Long = 2L

    override fun observeFavorites(): Flow<Set<Long>> = favoritesFlow.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value + contactId
    }

    override suspend fun removeFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value - contactId
    }
}

class MergeContactsUseCaseTest {
    @Test
    fun `merge delegates to repository without the target among sources`() =
        runTest {
            val repository = MergeFakeRepository()
            val useCase = MergeContactsUseCase(repository)

            val link = useCase(targetId = 7, sourceIds = listOf(8, 7, 9))

            assertEquals(listOf(7L to listOf(8L, 9L)), repository.merged)
            assertEquals(ContactLink(7L, listOf(8L, 9L)), link)
        }

    @Test
    fun `merge without sources does nothing`() =
        runTest {
            val repository = MergeFakeRepository()
            val useCase = MergeContactsUseCase(repository)

            val link = useCase(targetId = 7, sourceIds = listOf(7))

            assertEquals(emptyList<Pair<Long, List<Long>>>(), repository.merged)
            assertNull(link)
        }

    @Test
    fun `merge failure is reported to the caller`() =
        runTest {
            val repository = MergeFakeRepository(failMerge = true)
            val useCase = MergeContactsUseCase(repository)

            val failure = runCatching { useCase(targetId = 7, sourceIds = listOf(8)) }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
        }

    @Test
    fun `undo unlinks the merged contacts`() =
        runTest {
            val repository = MergeFakeRepository()
            val useCase = MergeContactsUseCase(repository)
            val unmerge = UnmergeContactsUseCase(repository)
            val link = useCase(targetId = 7, sourceIds = listOf(8))

            unmerge(requireNotNull(link))

            assertEquals(listOf(ContactLink(7L, listOf(8L))), repository.unmerged)
        }
}
