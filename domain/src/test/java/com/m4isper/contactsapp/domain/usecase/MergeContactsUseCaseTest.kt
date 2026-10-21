package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class MergeFakeRepository(
    initialFavorites: Set<Long> = emptySet(),
    private val failMerge: Boolean = false,
) : ContactsRepository,
    FavoritesRepository {
    val merged = mutableListOf<Pair<Long, List<Long>>>()
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
    ) {
        if (failMerge) error("merge failed")
        merged += targetId to sourceIds
    }

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
            val useCase = MergeContactsUseCase(repository, repository)

            useCase(targetId = 7, sourceIds = listOf(8, 7, 9))

            assertEquals(listOf(7L to listOf(8L, 9L)), repository.merged)
        }

    @Test
    fun `merge without sources does nothing`() =
        runTest {
            val repository = MergeFakeRepository()
            val useCase = MergeContactsUseCase(repository, repository)

            useCase(targetId = 7, sourceIds = listOf(7))

            assertEquals(emptyList<Pair<Long, List<Long>>>(), repository.merged)
        }

    @Test
    fun `favorite source makes the target favorite and is cleaned up`() =
        runTest {
            val repository = MergeFakeRepository(initialFavorites = setOf(8L))
            val useCase = MergeContactsUseCase(repository, repository)

            useCase(targetId = 7, sourceIds = listOf(8))

            assertEquals(setOf(7L), repository.favoritesFlow.value)
        }

    @Test
    fun `favorite target is kept as is`() =
        runTest {
            val repository = MergeFakeRepository(initialFavorites = setOf(7L, 8L))
            val useCase = MergeContactsUseCase(repository, repository)

            useCase(targetId = 7, sourceIds = listOf(8))

            assertEquals(setOf(7L), repository.favoritesFlow.value)
        }

    @Test
    fun `merge failure leaves favorites untouched`() =
        runTest {
            val repository = MergeFakeRepository(initialFavorites = setOf(8L), failMerge = true)
            val useCase = MergeContactsUseCase(repository, repository)

            val failure = runCatching { useCase(targetId = 7, sourceIds = listOf(8)) }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertEquals(setOf(8L), repository.favoritesFlow.value)
        }
}
