package com.m4isper.contactsapp.presentation.details

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
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

private class FakeDetailsRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var failure: Throwable? = null,
    initialFavorites: Set<Long> = emptySet(),
) : ContactsRepository,
    FavoritesRepository {
    private val favoritesFlow = MutableStateFlow(initialFavorites)

    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
        failure?.let { throw it }
        return contacts.firstOrNull { it.contactId == contactId }
    }

    override fun observeFavorites(): Flow<Set<Long>> = favoritesFlow.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) {
        val current = favoritesFlow.value
        favoritesFlow.value =
            if (contactId in current) {
                current - contactId
            } else {
                current + contactId
            }
    }

    override suspend fun removeFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value - contactId
    }

    override suspend fun createContact(form: ContactForm) = Unit

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeDetailsRepository) =
        ContactDetailsViewModel(
            GetContactDetailsUseCase(repository),
            ToggleFavoriteUseCase(repository),
            ObserveFavoritesUseCase(repository),
        )

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
            assertTrue(content is Loadable.Content)
            assertEquals("Anna", (content as Loadable.Content).data.name)
        }

    @Test
    fun `load unknown id emits not found`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel(FakeDetailsRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(42)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Empty)
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
            assertTrue(content is Loadable.Error)
            assertEquals("denied", (content as Loadable.Error).message)
        }

    @Test
    fun `load different id refetches`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            var calls = 0
            val repository =
                object : ContactsRepository, FavoritesRepository {
                    override suspend fun getContacts(): List<ContactDetails> = emptyList()

                    override suspend fun getContactForm(contactId: Long): ContactForm? = null

                    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
                        calls++
                        return null
                    }

                    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet())

                    override suspend fun toggleFavorite(contactId: Long) = Unit

                    override suspend fun removeFavorite(contactId: Long) = Unit

                    override suspend fun createContact(form: ContactForm) = Unit

                    override suspend fun updateContact(
                        contactId: Long,
                        form: ContactForm,
                    ) = Unit

                    override suspend fun deleteContact(contactId: Long) = Unit
                }
            val viewModel =
                ContactDetailsViewModel(
                    GetContactDetailsUseCase(repository),
                    ToggleFavoriteUseCase(repository),
                    ObserveFavoritesUseCase(repository),
                )
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()
            viewModel.load(8)
            advanceUntilIdle()

            assertEquals(2, calls)
        }

    @Test
    fun `toggle favorite flips star state`() =
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
            assertEquals(false, viewModel.uiState.value.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.isFavorite)
        }
}
