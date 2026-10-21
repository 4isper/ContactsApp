package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.FakeContactsChangeSource
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.ContactRelation
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

/** Технический сбой удаления одного контакта, независимый от [FakeContactsRepository.failure]. */
private class DeleteFailedException : RuntimeException("delete failed")

private class FakeContactsRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var failure: Throwable? = null,
    initialFavorites: Set<Long> = emptySet(),
    var accounts: List<ContactAccount> = listOf(ContactAccount.Local),
) : ContactsRepository,
    FavoritesRepository {
    private val favoritesFlow = MutableStateFlow(initialFavorites)
    val deleted = mutableListOf<Long>()
    val created = mutableListOf<ContactForm>()

    /** Удаление этих контактов падает технически (в отличие от [failure], не влияет на загрузку). */
    val failDeleteFor = mutableSetOf<Long>()

    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
        failure?.let { throw it }
        return contacts.firstOrNull { it.contactId == contactId }
    }

    override suspend fun getContactAccounts(): List<ContactAccount> {
        failure?.let { throw it }
        return accounts
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

    override suspend fun createContact(form: ContactForm) {
        failure?.let { throw it }
        created += form
        val newId = (contacts.maxOfOrNull { it.contactId } ?: 0L) + 1
        val name = form.name.display.ifBlank { form.name.composed }
        contacts = contacts + ContactDetails(contactId = newId, name = name)
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) {
        failure?.let { throw it }
        if (contactId in failDeleteFor) throw DeleteFailedException()
        deleted += contactId
        contacts = contacts.filterNot { it.contactId == contactId }
    }

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ) {
        failure?.let { throw it }
        contacts = contacts.filterNot { it.contactId in sourceIds }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
// Подавление осознанное: тест-аггрегатор поведения списка, декомпозиция по файлам не нужна.
@Suppress("TooManyFunctions", "LargeClass")
class ContactsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeContactsRepository,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
        codec: FakeVCardCodec = FakeVCardCodec(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        changes: FakeContactsChangeSource = FakeContactsChangeSource(),
    ) = ContactsViewModel(
        GetContactsUseCase(repository),
        GetContactAccountsUseCase(repository),
        ObserveFavoritesUseCase(repository),
        ObserveSettingsUseCase(settings),
        GetContactDetailsUseCase(repository),
        DeleteContactsUseCase(repository, repository),
        ImportContactsUseCase(CreateContactUseCase(repository)),
        GetGroupsUseCase(groups),
        GetGroupMembershipUseCase(groups),
        CreateGroupUseCase(groups),
        AddContactsToGroupUseCase(groups),
        ToggleFavoriteUseCase(repository),
        RemoveFavoriteUseCase(repository),
        codec,
        changes,
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
    fun `provider change refreshes contacts automatically`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val changes = FakeContactsChangeSource()
            val viewModel = viewModel(repository, changes = changes)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            repository.contacts = repository.contacts + details(2, "Bob", "222")
            viewModel.startObservingChanges()
            advanceUntilIdle()
            changes.notifyChange()
            advanceTimeBy(CONTACTS_CHANGE_DEBOUNCE_MILLIS + 1)
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(
                listOf("Anna", "Bob"),
                (content as Loadable.Content).data.map { it.name },
            )
        }

    @Test
    fun `provider change keeps current selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                )
            val changes = FakeContactsChangeSource()
            val viewModel = viewModel(repository, changes = changes)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(1)
            advanceUntilIdle()

            viewModel.startObservingChanges()
            advanceUntilIdle()
            changes.notifyChange()
            advanceTimeBy(CONTACTS_CHANGE_DEBOUNCE_MILLIS + 1)
            advanceUntilIdle()

            assertEquals(setOf(1L), viewModel.uiState.value.selection)
        }

    @Test
    fun `stopped observer ignores provider changes`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val changes = FakeContactsChangeSource()
            val viewModel = viewModel(repository, changes = changes)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.startObservingChanges()
            advanceUntilIdle()
            viewModel.stopObservingChanges()

            repository.contacts = repository.contacts + details(2, "Bob", "222")
            changes.notifyChange()
            advanceTimeBy(CONTACTS_CHANGE_DEBOUNCE_MILLIS + 1)
            advanceUntilIdle()

            val content = viewModel.uiState.value.content as Loadable.Content
            assertEquals(listOf("Anna"), content.data.map { it.name })
        }

    @Test
    fun `settings land in ui state as sort order and density`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings =
                FakeSettingsRepository(
                    AppSettings(
                        sortOrder = ContactSortOrder.BY_FAMILY,
                        listDensity = ListDensity.COMPACT,
                    ),
                )
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository, settings = settings)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(ContactSortOrder.BY_FAMILY, viewModel.uiState.value.sortOrder)
            assertEquals(ListDensity.COMPACT, viewModel.uiState.value.listDensity)
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
    fun `search filters by nickname and relation names`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        ContactDetails(contactId = 1, name = "Anna", nickname = "Нюша"),
                        ContactDetails(
                            contactId = 2,
                            name = "Bob",
                            relations = listOf(ContactRelation(name = "Михаил", kind = RelationKind.FRIEND)),
                        ),
                        details(3, "Cara", "333"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("нюша")
            advanceUntilIdle()
            val byNickname = viewModel.uiState.value.content
            assertTrue(byNickname is Loadable.Content)
            assertEquals(
                listOf("Anna"),
                (byNickname as Loadable.Content).data.map { it.name },
            )

            viewModel.onSearchQueryChange("михаил")
            advanceUntilIdle()
            val byRelation = viewModel.uiState.value.content
            assertTrue(byRelation is Loadable.Content)
            assertEquals(
                listOf("Bob"),
                (byRelation as Loadable.Content).data.map { it.name },
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

    @Test
    fun `quick favorite toggles contact in state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onToggleFavorite(1)
            advanceUntilIdle()
            assertEquals(setOf(1L), viewModel.uiState.value.favorites)

            viewModel.onToggleFavorite(1)
            advanceUntilIdle()
            assertEquals(emptySet<Long>(), viewModel.uiState.value.favorites)
        }

    @Test
    fun `batch favorite adds selected and removes when all are favorites`() =
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
            viewModel.onContactSelected(1)
            viewModel.onContactSelected(2)

            viewModel.onToggleSelectedFavorite()
            advanceUntilIdle()
            assertEquals(setOf(1L, 2L), viewModel.uiState.value.favorites)

            viewModel.onToggleSelectedFavorite()
            advanceUntilIdle()
            assertEquals(emptySet<Long>(), viewModel.uiState.value.favorites)
        }

    @Test
    fun `pinned favorites are disabled by search or filters`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository, settings = settings)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.pinFavorites)

            viewModel.onSearchQueryChange("Anna")
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)

            viewModel.onSearchQueryChange("")
            viewModel.onToggleFavoritesOnly()
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)

            viewModel.onToggleFavoritesOnly()
            viewModel.onGroupSelected(42)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)

            viewModel.onGroupSelected(null)
            viewModel.onAccountSelected(ContactAccount(name = "Work", type = "com.example"))
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)
        }

    @Test
    fun `favorites first setting disables pinned section`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository(AppSettings(favoritesFirst = false))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository, settings = settings)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.pinFavorites)
        }

    @Test
    fun `long press enters selection and row taps toggle it`() =
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

            viewModel.onContactSelected(1)
            advanceUntilIdle()
            assertEquals(setOf(1L), viewModel.uiState.value.selection)
            assertTrue(viewModel.uiState.value.isSelectionMode)

            viewModel.onContactSelected(2)
            viewModel.onContactSelected(1)
            advanceUntilIdle()
            assertEquals(setOf(2L), viewModel.uiState.value.selection)
        }

    @Test
    fun `clear selection exits selection mode`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(1)
            advanceUntilIdle()

            viewModel.onClearSelection()
            advanceUntilIdle()

            assertEquals(emptySet<Long>(), viewModel.uiState.value.selection)
            assertEquals(false, viewModel.uiState.value.isSelectionMode)
        }

    @Test
    fun `select all toggles whole visible list`() =
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

            viewModel.onSelectAll(listOf(1, 2))
            advanceUntilIdle()
            assertEquals(setOf(1L, 2L), viewModel.uiState.value.selection)

            viewModel.onSelectAll(listOf(1, 2))
            advanceUntilIdle()
            assertEquals(emptySet<Long>(), viewModel.uiState.value.selection)
        }

    @Test
    fun `search query change clears selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(1)
            advanceUntilIdle()

            viewModel.onSearchQueryChange("ann")
            advanceUntilIdle()

            assertEquals(emptySet<Long>(), viewModel.uiState.value.selection)
        }

    @Test
    fun `favorites toggle clears selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(1)
            advanceUntilIdle()

            viewModel.onToggleFavoritesOnly()
            advanceUntilIdle()

            assertEquals(emptySet<Long>(), viewModel.uiState.value.selection)
        }

    @Test
    fun `delete selected removes contacts favorites and selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                    initialFavorites = setOf(1, 2),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onContactSelected(1)
            viewModel.onContactSelected(2)
            advanceUntilIdle()
            viewModel.onDeleteSelected()
            runCurrent()
            viewModel.onUndoDelete()
            advanceUntilIdle()

            assertEquals(emptyList<Long>(), repository.deleted)
            val state = viewModel.uiState.value
            assertEquals(emptySet<Long>(), state.selection)
            assertEquals(setOf(1L, 2L), state.favorites)
            val content = state.content
            assertTrue(content is Loadable.Content)
            assertEquals(listOf("Anna", "Bob"), (content as Loadable.Content).data.map { it.name })
        }

    @Test
    fun `delete selected stages contacts and hides them until timeout`() =
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
            viewModel.onContactSelected(1)
            runCurrent()

            viewModel.onDeleteSelected()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(setOf(1L), state.pendingDeleteIds)
            assertEquals(false, state.deleteClosing)
            assertEquals(emptyList<Long>(), repository.deleted)
            val content = state.content
            assertTrue(content is Loadable.Content)
            assertEquals(listOf("Bob"), (content as Loadable.Content).data.map { it.name })
        }

    @Test
    fun `undo delete restores contacts and skips repository`() =
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
            viewModel.onContactSelected(1)
            viewModel.onDeleteSelected()
            runCurrent()
            assertEquals(setOf(1L), viewModel.uiState.value.pendingDeleteIds)

            viewModel.onUndoDelete()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(emptySet<Long>(), state.pendingDeleteIds)
            assertEquals(false, state.deleteClosing)
            assertEquals(null, state.event)
            assertEquals(emptyList<Long>(), repository.deleted)
            val content = state.content
            assertTrue(content is Loadable.Content)
            assertEquals(listOf("Anna", "Bob"), (content as Loadable.Content).data.map { it.name })
        }

    @Test
    fun `staged delete commits after undo timeout`() =
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
            viewModel.onContactSelected(1)
            viewModel.onContactSelected(2)
            viewModel.onDeleteSelected()
            runCurrent()
            assertEquals(setOf(1L, 2L), viewModel.uiState.value.pendingDeleteIds)

            advanceTimeBy(DELETE_UNDO_TIMEOUT_MILLIS + 1)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(emptySet<Long>(), state.pendingDeleteIds)
            assertEquals(listOf(1L, 2L), repository.deleted)
            assertTrue(state.content is Loadable.Empty)
        }

    @Test
    fun `delete window closes after commit and undo is a no-op then`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(1)
            viewModel.onDeleteSelected()
            runCurrent()

            advanceTimeBy(DELETE_UNDO_TIMEOUT_MILLIS + 1)
            runCurrent()
            assertEquals(true, viewModel.uiState.value.deleteClosing)

            // UI гасит snackbar и сообщает о закрытии окна.
            viewModel.onDeleteWindowClosed()
            runCurrent()
            assertEquals(false, viewModel.uiState.value.deleteClosing)

            // После коммита отмена ничего не возвращает и не трогает список.
            viewModel.onUndoDelete()
            runCurrent()
            assertEquals(listOf(1L), repository.deleted)
            assertTrue(viewModel.uiState.value.content is Loadable.Empty)
        }

    @Test
    fun `partial delete keeps failed contacts and reports failure`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                ).apply { failDeleteFor += 2L }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(1)
            viewModel.onContactSelected(2)
            viewModel.onDeleteSelected()
            runCurrent()

            advanceTimeBy(DELETE_UNDO_TIMEOUT_MILLIS + 1)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(ContactsEvent.DeleteFailed, state.event)
            assertEquals(listOf(1L), repository.deleted)
            val content = state.content
            assertTrue(content is Loadable.Content)
            assertEquals(listOf("Bob"), (content as Loadable.Content).data.map { it.name })
        }

    @Test
    fun `second delete during undo window extends the window`() =
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
            viewModel.onContactSelected(1)
            viewModel.onDeleteSelected()
            runCurrent()
            advanceTimeBy(DELETE_UNDO_TIMEOUT_MILLIS / 2)
            runCurrent()

            viewModel.onContactSelected(2)
            viewModel.onDeleteSelected()
            runCurrent()
            assertEquals(setOf(1L, 2L), viewModel.uiState.value.pendingDeleteIds)

            // Половина исходного окна прошла, но окно перезапущено: пока удалений не было.
            advanceTimeBy(DELETE_UNDO_TIMEOUT_MILLIS / 2 + 1)
            runCurrent()
            assertEquals(emptyList<Long>(), repository.deleted)

            advanceTimeBy(DELETE_UNDO_TIMEOUT_MILLIS + 1)
            runCurrent()
            assertEquals(listOf(1L, 2L), repository.deleted)
        }

    @Test
    fun `share selected emits details and handled event clears it`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                )
            val codec = FakeVCardCodec(encoded = "shared-vcard")
            val viewModel = viewModel(repository, codec = codec)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(2)
            advanceUntilIdle()

            viewModel.onShareSelected()
            advanceUntilIdle()

            val event = viewModel.uiState.value.event
            assertTrue(event is ContactsEvent.Share)
            assertEquals("shared-vcard", (event as ContactsEvent.Share).content)
            // Шарится только выделенный контакт: детали остальных в порт не уходят.
            assertEquals(listOf(2L), codec.encodedContacts.single().map { it.contactId })

            viewModel.onEventHandled()
            advanceUntilIdle()
            assertEquals(null, viewModel.uiState.value.event)
        }

    @Test
    fun `import creates contacts and emits finished event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository()
            val codec = FakeVCardCodec(forms = listOf(ContactForm(name = EditableName(display = "Anna"))))
            val viewModel = viewModel(repository, codec = codec)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            val text =
                """
                BEGIN:VCARD
                VERSION:3.0
                FN:Anna
                TEL;TYPE=CELL:+71112223344
                END:VCARD
                """.trimIndent()
            viewModel.files.onImportText(text)
            advanceUntilIdle()

            // Текст без изменений уходит в порт, создаётся каждая разобранная форма.
            assertEquals(listOf(text), codec.parsedTexts)
            assertEquals(1, repository.created.size)
            val state = viewModel.uiState.value
            assertEquals(ContactsEvent.ImportFinished(1), state.event)
            assertTrue(state.content is Loadable.Content)
        }

    @Test
    fun `import without vcards emits empty event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.files.onImportText("plain text without vcards")
            advanceUntilIdle()

            assertEquals(ContactsEvent.ImportEmpty, viewModel.uiState.value.event)
            assertTrue(repository.created.isEmpty())
        }

    @Test
    fun `import failure emits failed event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(failure = RuntimeException("boom"))
            val viewModel = viewModel(repository, codec = FakeVCardCodec(forms = listOf(ContactForm())))
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.files.onImportText(
                """
                BEGIN:VCARD
                FN:Anna
                END:VCARD
                """.trimIndent(),
            )
            advanceUntilIdle()

            assertEquals(ContactsEvent.ImportFailed, viewModel.uiState.value.event)
        }

    @Test
    fun `export emits vcard content for all contacts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                )
            val codec = FakeVCardCodec(encoded = "exported-vcard")
            val viewModel = viewModel(repository, codec = codec)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.files.onExportRequested()
            advanceUntilIdle()

            val event = viewModel.uiState.value.event
            assertTrue(event is ContactsEvent.ExportReady)
            val ready = event as ContactsEvent.ExportReady
            assertEquals(2, ready.count)
            assertEquals("exported-vcard", ready.content)
            // В порт уходят полные детали всех контактов списка.
            assertEquals(listOf(1L, 2L), codec.encodedContacts.single().map { it.contactId })
        }

    @Test
    fun `export without contacts emits empty event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.files.onExportRequested()
            advanceUntilIdle()

            assertEquals(ContactsEvent.ExportEmpty, viewModel.uiState.value.event)
        }

    @Test
    fun `export failure emits failed event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            repository.failure = RuntimeException("boom")

            viewModel.files.onExportRequested()
            advanceUntilIdle()

            assertEquals(ContactsEvent.ExportFailed, viewModel.uiState.value.event)
        }

    @Test
    fun `clear resets search favorites and failure`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            repository.failure = RuntimeException("boom")
            viewModel.refresh()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.content is Loadable.Error)

            viewModel.onSearchQueryChange("ann")
            viewModel.onToggleFavoritesOnly()
            advanceUntilIdle()

            viewModel.clear()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.searchQuery)
            assertEquals(false, state.showFavoritesOnly)
            assertEquals(emptySet<Long>(), state.selection)
            assertTrue(state.content is Loadable.Empty)
        }

    @Test
    fun `refresh loads groups and group filter shows only member contacts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                )
            val groups =
                FakeGroupsRepository(
                    groups = listOf(ContactGroup(id = 100, title = "Семья")),
                    membership = mapOf(1L to setOf(100L)),
                )
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onGroupSelected(100)
            advanceUntilIdle()

            val filtered = viewModel.uiState.value
            assertEquals(listOf(ContactGroup(id = 100, title = "Семья")), filtered.groups)
            assertEquals(100L, filtered.selectedGroupId)
            assertEquals(listOf("Anna"), (filtered.content as Loadable.Content).data.map { it.name })

            viewModel.onGroupSelected(null)
            advanceUntilIdle()

            assertEquals(2, (viewModel.uiState.value.content as Loadable.Content).data.size)
        }

    @Test
    fun `refresh loads accounts and account filter limits list`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val emptyAccount = ContactAccount(name = "empty@example.com", type = "com.google")
            val repository =
                FakeContactsRepository(
                    contacts =
                        listOf(
                            details(1, "Anna", "111"),
                            details(2, "Bob", "222").copy(account = google),
                        ),
                    accounts = listOf(ContactAccount.Local, google, emptyAccount),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            val loaded = viewModel.uiState.value
            assertEquals(listOf(ContactAccount.Local, google, emptyAccount), loaded.accounts)
            assertEquals(2, (loaded.content as Loadable.Content).data.size)

            viewModel.onAccountSelected(google)
            advanceUntilIdle()

            val filtered = viewModel.uiState.value
            assertEquals(google, filtered.selectedAccount)
            assertEquals(listOf("Bob"), (filtered.content as Loadable.Content).data.map { it.name })

            viewModel.onAccountSelected(emptyAccount)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Empty)

            viewModel.onAccountSelected(null)
            advanceUntilIdle()

            assertEquals(2, (viewModel.uiState.value.content as Loadable.Content).data.size)
        }

    @Test
    fun `deleted group is dropped from active filter`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onGroupSelected(100)
            advanceUntilIdle()

            groups.groups = emptyList()
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.selectedGroupId)
        }

    @Test
    fun `add selected to group records ids emits event and clears selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        details(1, "Anna", "111"),
                        details(2, "Bob", "222"),
                    ),
                )
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onContactSelected(1)
            viewModel.onContactSelected(2)
            viewModel.onAddSelectedToGroup(setOf(100))
            advanceUntilIdle()

            assertEquals(listOf(100L to listOf(1L, 2L)), groups.addedToGroup)
            val state = viewModel.uiState.value
            assertEquals(ContactsEvent.GroupAddFinished(2), state.event)
            assertEquals(emptySet<Long>(), state.selection)
        }

    @Test
    fun `add to group failure emits failed event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(details(1, "Anna", "111")))
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.onContactSelected(1)
            groups.failure = RuntimeException("boom")

            viewModel.onAddSelectedToGroup(setOf(100))
            advanceUntilIdle()

            assertEquals(ContactsEvent.GroupActionFailed, viewModel.uiState.value.event)
        }

    @Test
    fun `create group refreshes the groups list`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups = FakeGroupsRepository()
            val viewModel = viewModel(FakeContactsRepository(), groups)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onCreateGroup("Семья")
            advanceUntilIdle()

            assertEquals(listOf(ContactGroup(id = 1, title = "Семья")), viewModel.uiState.value.groups)
        }

    @Test
    fun `create group failure emits failed event`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups = FakeGroupsRepository(failure = RuntimeException("boom"))
            val viewModel = viewModel(FakeContactsRepository(), groups)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.onCreateGroup("Семья")
            advanceUntilIdle()

            assertEquals(ContactsEvent.GroupActionFailed, viewModel.uiState.value.event)
        }
}
