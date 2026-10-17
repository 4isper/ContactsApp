package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
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

private fun details(
    id: Long,
    name: String,
    vararg phones: String,
) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
)

private class FakeContactsRepository(
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
// Подавление осознанное: тест-аггрегатор поведения списка, декомпозиция по файлам не нужна.
@Suppress("TooManyFunctions")
class ContactsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeContactsRepository) =
        ContactsViewModel(
            GetContactsUseCase(repository),
            ObserveFavoritesUseCase(repository),
        )

    @Test
    fun `refresh emits loaded contacts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111", "112"),
                        details(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(2, (content as Loadable.Content).data.size)
        }

    @Test
    fun `search filters by name case-insensitively`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("ANN")
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(
                listOf("Anna"),
                (content as Loadable.Content).data.map { it.name },
            )
        }

    @Test
    fun `search filters by phone number`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("222")
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(
                listOf("Bob"),
                (content as Loadable.Content).data.map { it.name },
            )
        }

    @Test
    fun `search filters by email`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        ContactDetails(
                            contactId = 2,
                            name = "Bob",
                            emails = listOf(EmailAddress("bob@example.com", EmailKind.OTHER)),
                        ),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("bob@")
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(
                listOf("Bob"),
                (content as Loadable.Content).data.map { it.name },
            )
        }

    @Test
    fun `search filters by structured name components`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 1,
                            name = "Петров Иван",
                            nameParts = NameParts(given = "Иван", family = "Петров", middle = "Петрович"),
                        ),
                        details(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("петрович")
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(
                listOf("Петров Иван"),
                (content as Loadable.Content).data.map { it.name },
            )
        }

    @Test
    fun `search filters by phonetic name`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 1,
                            name = "Иван Петров",
                            nameParts =
                                NameParts(
                                    given = "Иван",
                                    family = "Петров",
                                    phoneticGiven = "Ivan",
                                ),
                        ),
                        details(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("ivan")
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(
                listOf("Иван Петров"),
                (content as Loadable.Content).data.map { it.name },
            )
        }

    @Test
    fun `search with no match emits searching empty`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(details(1, "Anna", "111")),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("zzz")
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Empty)
            assertTrue(viewModel.uiState.value.isSearching)
        }

    @Test
    fun `refresh failure emits error with cause message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(failure = SecurityException("denied"))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Error)
            assertEquals("denied", (content as Loadable.Error).message)
        }

    @Test
    fun `refresh failure without message emits error with null message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(failure = RuntimeException())
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Error)
            assertEquals(null, (content as Loadable.Error).message)
        }

    @Test
    fun `clear resets list to non-searching empty`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(details(1, "Anna", "111")),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.clear()
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Empty)
            assertEquals(false, viewModel.uiState.value.isSearching)
        }

    @Test
    fun `favorites only filter shows only favorites`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                    initialFavorites = setOf(2),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onToggleFavoritesOnly()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.showFavoritesOnly)
            val content = state.content
            assertTrue(content is Loadable.Content)
            assertEquals(listOf("Bob"), (content as Loadable.Content).data.map { it.name })
        }

    @Test
    fun `favorites only with no favorites emits empty`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(details(1, "Anna", "111")),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onToggleFavoritesOnly()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.content is Loadable.Empty)
            assertEquals(false, state.isSearching)
        }
}
