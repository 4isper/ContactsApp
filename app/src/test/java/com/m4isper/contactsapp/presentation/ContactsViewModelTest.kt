package com.m4isper.contactsapp.presentation

import com.m4isper.contactsapp.domain.model.Contact
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private class FakeContactsRepository(
    var contacts: List<Contact> = emptyList(),
    var failure: Throwable? = null,
) : ContactsRepository {
    override suspend fun getContacts(): List<Contact> {
        failure?.let { throw it }
        return contacts
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeContactsRepository) =
        ContactsViewModel(GetContactsUseCase(repository))

    @Test
    fun `refresh emits loaded contacts`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeContactsRepository(
            listOf(
                Contact(id = 1, name = "Anna", phone = "111"),
                Contact(id = 2, name = "Bob", phone = "222"),
            ),
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.refresh()
        advanceUntilIdle()

        val content = viewModel.uiState.value.content
        assertTrue(content is ContactsContent.Contacts)
        assertEquals(2, (content as ContactsContent.Contacts).items.size)
    }

    @Test
    fun `search filters by name case-insensitively`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeContactsRepository(
            listOf(
                Contact(id = 1, name = "Anna", phone = "111"),
                Contact(id = 2, name = "Bob", phone = "222"),
            ),
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("ANN")
        advanceUntilIdle()

        val content = viewModel.uiState.value.content
        assertTrue(content is ContactsContent.Contacts)
        assertEquals(
            listOf("Anna"),
            (content as ContactsContent.Contacts).items.map { it.name },
        )
    }

    @Test
    fun `search with no match emits searching empty`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeContactsRepository(
            listOf(Contact(id = 1, name = "Anna", phone = "111")),
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("zzz")
        advanceUntilIdle()

        val content = viewModel.uiState.value.content
        assertTrue(content is ContactsContent.Empty)
        assertTrue((content as ContactsContent.Empty).isSearching)
    }

    @Test
    fun `refresh failure emits error with cause message`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeContactsRepository(failure = SecurityException("denied"))
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.refresh()
        advanceUntilIdle()

        val content = viewModel.uiState.value.content
        assertTrue(content is ContactsContent.Error)
        assertEquals("denied", (content as ContactsContent.Error).message)
    }

    @Test
    fun `refresh failure without message emits error with null message`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeContactsRepository(failure = RuntimeException())
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.refresh()
        advanceUntilIdle()

        val content = viewModel.uiState.value.content
        assertTrue(content is ContactsContent.Error)
        assertEquals(null, (content as ContactsContent.Error).message)
    }

    @Test
    fun `clear resets list to non-searching empty`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeContactsRepository(
            listOf(Contact(id = 1, name = "Anna", phone = "111")),
        )
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.clear()
        advanceUntilIdle()

        val content = viewModel.uiState.value.content
        assertTrue(content is ContactsContent.Empty)
        assertEquals(false, (content as ContactsContent.Empty).isSearching)
    }
}
