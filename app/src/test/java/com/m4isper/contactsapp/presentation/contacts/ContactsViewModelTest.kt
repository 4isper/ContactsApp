package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.FakeContactsChangeSource
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.ContactRelation
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.NameParts
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.contacts.effects.DELETE_UNDO_TIMEOUT_MILLIS
import com.m4isper.contactsapp.testing.ContactsTestGraph
import com.m4isper.contactsapp.testing.FakeContactsRepository
import com.m4isper.contactsapp.testing.testDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
// Подавление осознанное: тест-аггрегатор поведения списка, декомпозиция по файлам не нужна.
@Suppress("TooManyFunctions", "LargeClass") // Осознанно: тест закрывает весь экран: загрузка, выбор, группы, избранное.
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
    ) = ContactsTestGraph(
        repository = repository,
        groups = groups,
        settings = settings,
        changes = changes,
        codec = codec,
    ).viewModel()

    @Test
    fun `contacts are loaded page by page`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                        testDetails(3, "Cara", "333"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals(3, (content as Loadable.Content).data.size)
        }

    @Test
    fun `refresh emits loaded contacts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        testDetails(1, "Anna", "111", "112"),
                        testDetails(2, "Bob", "222"),
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
    fun `initial load shows loading without refresh indicator`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(listOf(testDetails(1, "Anna", "111"))).apply {
                    pageLoadDelayMillis = 1_000
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(false, state.isRefreshing)
            assertTrue(state.content is Loadable.Loading)
            advanceUntilIdle()
        }

    @Test
    fun `background refresh keeps loaded content and hides refresh indicator`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            repository.pageLoadDelayMillis = 1_000

            viewModel.refresh()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(false, state.isRefreshing)
            assertTrue(state.content is Loadable.Content)
            advanceUntilIdle()
        }

    @Test
    fun `background refresh keeps empty state without loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository()
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            repository.pageLoadDelayMillis = 1_000

            viewModel.refresh()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(false, state.isRefreshing)
            assertTrue(state.content is Loadable.Empty)
            advanceUntilIdle()
        }

    @Test
    fun `manual refresh shows indicator without replacing loaded content`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            repository.pageLoadDelayMillis = 1_000

            viewModel.refresh(showRefreshIndicator = true)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(true, state.isRefreshing)
            assertTrue(state.content is Loadable.Content)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `provider change refreshes contacts automatically`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val changes = FakeContactsChangeSource()
            val viewModel = viewModel(repository, changes = changes)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            repository.contacts = repository.contacts + testDetails(2, "Bob", "222")
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val changes = FakeContactsChangeSource()
            val viewModel = viewModel(repository, changes = changes)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
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
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val changes = FakeContactsChangeSource()
            val viewModel = viewModel(repository, changes = changes)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.startObservingChanges()
            advanceUntilIdle()
            viewModel.stopObservingChanges()

            repository.contacts = repository.contacts + testDetails(2, "Bob", "222")
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
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onSearchQueryChange("ANN")
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onSearchQueryChange("222")
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
                        testDetails(1, "Anna", "111"),
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

            viewModel.filters.onSearchQueryChange("bob@")
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
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onSearchQueryChange("петрович")
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
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onSearchQueryChange("ivan")
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
                        testDetails(3, "Cara", "333"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onSearchQueryChange("нюша")
            advanceUntilIdle()
            val byNickname = viewModel.uiState.value.content
            assertTrue(byNickname is Loadable.Content)
            assertEquals(
                listOf("Anna"),
                (byNickname as Loadable.Content).data.map { it.name },
            )

            viewModel.filters.onSearchQueryChange("михаил")
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
                    listOf(testDetails(1, "Anna", "111")),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onSearchQueryChange("zzz")
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
            assertEquals(AppError.PERMISSION_DENIED, (content as Loadable.Error).error)
        }

    @Test
    fun `the cancellation of the load is not turned into an error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(failure = CancellationException("cancelled"))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            advanceUntilIdle()

            // Отмена корутины - не ошибка провайдера: список остаётся пустым.
            assertTrue(viewModel.uiState.value.content !is Loadable.Error)
        }

    @Test
    fun `refresh failure without message emits unknown error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(failure = RuntimeException())
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.refresh()
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Error)
            assertEquals(AppError.UNKNOWN, (content as Loadable.Error).error)
        }

    @Test
    fun `clear resets list to non-searching empty`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(testDetails(1, "Anna", "111")),
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                    initialFavorites = setOf(2),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onToggleFavoritesOnly()
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
                    listOf(testDetails(1, "Anna", "111")),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.filters.onToggleFavoritesOnly()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.content is Loadable.Empty)
            assertEquals(false, state.isSearching)
        }

    @Test
    fun `quick favorite toggles contact in state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.favorites.onToggleFavorite(1)
            advanceUntilIdle()
            assertEquals(setOf(1L), viewModel.uiState.value.favorites)

            viewModel.favorites.onToggleFavorite(1)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            viewModel.selection.onContactSelected(2)

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
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository, settings = settings)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.pinFavorites)

            viewModel.filters.onSearchQueryChange("Anna")
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)

            viewModel.filters.onSearchQueryChange("")
            viewModel.filters.onToggleFavoritesOnly()
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)

            viewModel.filters.onToggleFavoritesOnly()
            viewModel.filters.onGroupSelected(42)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)

            viewModel.filters.onGroupSelected(null)
            viewModel.filters.onAccountSelected(ContactAccount(name = "Work", type = "com.example"))
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.pinFavorites)
        }

    @Test
    fun `favorites first setting disables pinned section`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository(AppSettings(favoritesFirst = false))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.selection.onContactSelected(1)
            advanceUntilIdle()
            assertEquals(setOf(1L), viewModel.uiState.value.selection)
            assertTrue(viewModel.uiState.value.isSelectionMode)

            viewModel.selection.onContactSelected(2)
            viewModel.selection.onContactSelected(1)
            advanceUntilIdle()
            assertEquals(setOf(2L), viewModel.uiState.value.selection)
        }

    @Test
    fun `clear selection exits selection mode`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            advanceUntilIdle()

            viewModel.selection.clear()
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.selection.selectAll(listOf(1, 2))
            advanceUntilIdle()
            assertEquals(setOf(1L, 2L), viewModel.uiState.value.selection)

            viewModel.selection.selectAll(listOf(1, 2))
            advanceUntilIdle()
            assertEquals(emptySet<Long>(), viewModel.uiState.value.selection)
        }

    @Test
    fun `search query change clears selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            advanceUntilIdle()

            viewModel.filters.onSearchQueryChange("ann")
            advanceUntilIdle()

            assertEquals(emptySet<Long>(), viewModel.uiState.value.selection)
        }

    @Test
    fun `favorites toggle clears selection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            advanceUntilIdle()

            viewModel.filters.onToggleFavoritesOnly()
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                    initialFavorites = setOf(1, 2),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.selection.onContactSelected(1)
            viewModel.selection.onContactSelected(2)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            viewModel.selection.onContactSelected(2)
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
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                ).apply { failDeleteFor += 2L }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            viewModel.selection.onContactSelected(2)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            viewModel.onDeleteSelected()
            runCurrent()
            advanceTimeBy(DELETE_UNDO_TIMEOUT_MILLIS / 2)
            runCurrent()

            viewModel.selection.onContactSelected(2)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val codec = FakeVCardCodec(encoded = "shared-vcard")
            val viewModel = viewModel(repository, codec = codec)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(2)
            advanceUntilIdle()

            viewModel.onShareSelected()
            advanceUntilIdle()

            val event = viewModel.uiState.value.event
            assertTrue(event is ContactsEvent.Share)
            assertEquals("shared-vcard", (event as ContactsEvent.Share).content)
            // Шарится только выделенный контакт: детали остальных в порт не уходят.
            assertEquals(listOf(2L), codec.encodedContacts.single().map { it.details.contactId })

            viewModel.onEventHandled()
            advanceUntilIdle()
            assertEquals(null, viewModel.uiState.value.event)
        }

    @Test
    fun `export emits vcard content for all contacts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
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
            assertEquals(listOf(1L, 2L), codec.encodedContacts.single().map { it.details.contactId })
        }

    @Test
    fun `export selection emits vcard for selected contacts only`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeContactsRepository(
                    listOf(
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val codec = FakeVCardCodec(encoded = "selected-vcard")
            val viewModel = viewModel(repository, codec = codec)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(2)
            advanceUntilIdle()

            viewModel.onExportSelection()
            advanceUntilIdle()

            val event = viewModel.uiState.value.event
            assertTrue(event is ContactsEvent.ExportReady)
            val ready = event as ContactsEvent.ExportReady
            assertEquals(1, ready.count)
            assertEquals("selected-vcard", ready.content)
            assertEquals(listOf(2L), codec.encodedContacts.single().map { it.details.contactId })
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
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
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
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            repository.failure = RuntimeException("boom")
            viewModel.refresh()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.content is Loadable.Error)

            viewModel.filters.onSearchQueryChange("ann")
            viewModel.filters.onToggleFavoritesOnly()
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
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

            viewModel.filters.onGroupSelected(100)
            advanceUntilIdle()

            val filtered = viewModel.uiState.value
            assertEquals(listOf(ContactGroup(id = 100, title = "Семья")), filtered.groups)
            assertEquals(100L, filtered.selectedGroupId)
            assertEquals(listOf("Anna"), (filtered.content as Loadable.Content).data.map { it.name })

            viewModel.filters.onGroupSelected(null)
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
                            testDetails(1, "Anna", "111"),
                            testDetails(2, "Bob", "222").copy(account = google),
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

            viewModel.filters.onAccountSelected(google)
            advanceUntilIdle()

            val filtered = viewModel.uiState.value
            assertEquals(google, filtered.selectedAccount)
            assertEquals(listOf("Bob"), (filtered.content as Loadable.Content).data.map { it.name })

            viewModel.filters.onAccountSelected(emptyAccount)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Empty)

            viewModel.filters.onAccountSelected(null)
            advanceUntilIdle()

            assertEquals(2, (viewModel.uiState.value.content as Loadable.Content).data.size)
        }

    @Test
    fun `deleted group is dropped from active filter`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.filters.onGroupSelected(100)
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
                        testDetails(1, "Anna", "111"),
                        testDetails(2, "Bob", "222"),
                    ),
                )
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.selection.onContactSelected(1)
            viewModel.selection.onContactSelected(2)
            viewModel.groups.onAddSelectedToGroup(setOf(100))
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
            val repository = FakeContactsRepository(listOf(testDetails(1, "Anna", "111")))
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.refresh()
            advanceUntilIdle()
            viewModel.selection.onContactSelected(1)
            groups.failure = RuntimeException("boom")

            viewModel.groups.onAddSelectedToGroup(setOf(100))
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

            viewModel.groups.onCreateGroup("Семья")
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

            viewModel.groups.onCreateGroup("Семья")
            advanceUntilIdle()

            assertEquals(ContactsEvent.GroupActionFailed, viewModel.uiState.value.event)
        }
}
