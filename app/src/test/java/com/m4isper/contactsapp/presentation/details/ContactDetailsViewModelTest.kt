package com.m4isper.contactsapp.presentation.details

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
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

private class FakeDetailsRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var failure: Throwable? = null,
) : ContactsRepository {
    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
        failure?.let { throw it }
        return contacts.firstOrNull { it.contactId == contactId }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeDetailsRepository) =
        ContactDetailsViewModel(GetContactDetailsUseCase(repository))

    @Test
    fun `load emits contact content`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDetailsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 7,
                            name = "Anna",
                            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
                        ),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is DetailsContent.Content)
            assertEquals("Anna", (content as DetailsContent.Content).details.name)
        }

    @Test
    fun `load unknown id emits not found`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel(FakeDetailsRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(42)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is DetailsContent.NotFound)
        }

    @Test
    fun `load failure emits error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeDetailsRepository(failure = SecurityException("denied"))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is DetailsContent.Error)
            assertEquals("denied", (content as DetailsContent.Error).message)
        }

    @Test
    fun `load different id refetches`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            var calls = 0
            val repository =
                object : ContactsRepository {
                    override suspend fun getContacts(): List<ContactDetails> = emptyList()

                    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
                        calls++
                        return null
                    }
                }
            val viewModel = ContactDetailsViewModel(GetContactDetailsUseCase(repository))
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()
            viewModel.load(8)
            advanceUntilIdle()

            assertEquals(2, calls)
        }
}
