package com.m4isper.contactsapp.presentation.duplicates

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.MergeContactsUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun duplicatePair(): List<ContactDetails> =
    listOf(
        ContactDetails(contactId = 7, name = "Anna", phones = listOf(PhoneNumber("+7111222", PhoneKind.MOBILE))),
        ContactDetails(contactId = 8, name = "Anna", phones = listOf(PhoneNumber("+7111222", PhoneKind.MOBILE))),
    )

private class FakeDuplicatesRepository(
    var contacts: List<ContactDetails> = emptyList(),
    initialFavorites: Set<Long> = emptySet(),
) : ContactsRepository,
    FavoritesRepository {
    var failure: Throwable? = null
    var mergeFailure: Throwable? = null
    val merged = mutableListOf<Pair<Long, List<Long>>>()
    val favoritesFlow = MutableStateFlow(initialFavorites)

    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

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
        mergeFailure?.let { throw it }
        merged += targetId to sourceIds
        contacts = contacts.filterNot { it.contactId in sourceIds }
    }

    override fun observeFavorites(): Flow<Set<Long>> = favoritesFlow.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value + contactId
    }

    override suspend fun removeFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value - contactId
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DuplicatesViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeDuplicatesRepository) =
        DuplicatesViewModel(
            GetContactsUseCase(repository),
            MergeContactsUseCase(repository, repository),
        )

    @Test
    fun `load emits duplicate groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel(FakeDuplicatesRepository(contacts = duplicatePair()))
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load()
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            val members =
                (content as Loadable.Content)
                    .data
                    .single()
                    .members
                    .map { it.contactId }
            assertEquals(listOf(7L, 8L), members)
        }

    @Test
    fun `load without duplicates emits empty state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel(FakeDuplicatesRepository(contacts = duplicatePair().take(1)))
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Empty)
        }

    @Test
    fun `load failure emits error state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDuplicatesRepository(contacts = duplicatePair()).apply {
                    failure = RuntimeException("boom")
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Error)
        }

    @Test
    fun `merge delegates to the use case and reloads groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeDuplicatesRepository(contacts = duplicatePair())
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onMerge(targetId = 7, sourceIds = listOf(8))
            advanceUntilIdle()

            assertEquals(listOf(7L to listOf(8L)), repository.merged)
            assertEquals(DuplicatesEvent.Merged, viewModel.uiState.value.event)
            assertTrue(viewModel.uiState.value.content is Loadable.Empty)
        }

    @Test
    fun `merge keeps favorite status of sources on the target`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeDuplicatesRepository(contacts = duplicatePair(), initialFavorites = setOf(8L))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onMerge(targetId = 7, sourceIds = listOf(8))
            advanceUntilIdle()

            assertEquals(setOf(7L), repository.favoritesFlow.value)
        }

    @Test
    fun `merge failure emits an event and keeps the groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDuplicatesRepository(contacts = duplicatePair()).apply {
                    mergeFailure = RuntimeException("boom")
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load()
            advanceUntilIdle()

            viewModel.onMerge(targetId = 7, sourceIds = listOf(8))
            advanceUntilIdle()

            assertEquals(DuplicatesEvent.MergeFailed, viewModel.uiState.value.event)
            assertTrue(viewModel.uiState.value.content is Loadable.Content)
        }

    @Test
    fun `merge without sources does nothing`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeDuplicatesRepository(contacts = duplicatePair())
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onMerge(targetId = 7, sourceIds = emptyList())
            advanceUntilIdle()

            assertTrue(repository.merged.isEmpty())
        }

    @Test
    fun `handled event is cleared`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeDuplicatesRepository(contacts = duplicatePair())
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onMerge(targetId = 7, sourceIds = listOf(8))
            advanceUntilIdle()
            viewModel.onEventHandled()
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.event)
        }
}
