package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.contact.ContactLink
import com.m4isper.contactsapp.domain.model.contact.RawContactRef
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.contacts.MergeContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.UnmergeContactsUseCase
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
) : ContactsMerger,
    FavoritesRepository {
    val merged = mutableListOf<Pair<Long, List<Long>>>()
    val unmerged = mutableListOf<ContactLink>()
    val favoritesFlow = MutableStateFlow(initialFavorites)

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): ContactLink? {
        if (failMerge) error("merge failed")
        merged += targetId to sourceIds
        return ContactLink(
            target = RawContactRef(targetId),
            sources = sourceIds.map { RawContactRef(it) },
        )
    }

    override suspend fun unmergeContacts(link: ContactLink) {
        unmerged += link
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
            val useCase = MergeContactsUseCase(repository)

            val link = useCase(targetId = 7, sourceIds = listOf(8, 7, 9))

            assertEquals(listOf(7L to listOf(8L, 9L)), repository.merged)
            assertEquals(
                ContactLink(
                    target = RawContactRef(7L),
                    sources = listOf(RawContactRef(8L), RawContactRef(9L)),
                ),
                link,
            )
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

            assertEquals(
                listOf(
                    ContactLink(
                        target = RawContactRef(7L),
                        sources = listOf(RawContactRef(8L)),
                    ),
                ),
                repository.unmerged,
            )
        }
}
