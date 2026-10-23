package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeContactsChangeSource
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.usecase.contacts.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsPageUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetProfileContactIdUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.RefreshFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ToggleFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.contacts.components.ContactList
import com.m4isper.contactsapp.presentation.contacts.state.ContactsFavoriteState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsFilterState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsGroupState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsListState
import com.m4isper.contactsapp.presentation.contacts.state.ContactsSelectionState
import com.m4isper.contactsapp.testDetails
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ContactListUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val anna = testDetails(1, "Anna", "111")
    private val bob = testDetails(2, "Bob", "222")

    private fun viewModel(
        repository: FakeAndroidRepository,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ): ContactsViewModel {
        val selection = ContactsSelectionState()
        val filters = ContactsFilterState(selection)
        return ContactsViewModel(
            list = ContactsListState(GetContactsPageUseCase(repository), FakeContactsChangeSource()),
            filters = filters,
            selection = selection,
            favorites =
                ContactsFavoriteState(
                    observeFavorites = ObserveFavoritesUseCase(repository),
                    toggleFavorite = ToggleFavoriteUseCase(repository),
                    removeFavorite = RemoveFavoriteUseCase(repository),
                    refreshFavorites = RefreshFavoritesUseCase(repository),
                ),
            groups =
                ContactsGroupState(
                    getGroups = GetGroupsUseCase(groups),
                    getMembership = GetGroupMembershipUseCase(groups),
                    getAccounts = GetContactAccountsUseCase(repository),
                    getProfileContactId = GetProfileContactIdUseCase(repository),
                    createGroup = CreateGroupUseCase(groups),
                    addContactsToGroup = AddContactsToGroupUseCase(groups),
                    filters = filters,
                    selection = selection,
                ),
            deleteContactsUseCase = DeleteContactsUseCase(repository, repository),
            exportContactsUseCase =
                ExportContactsUseCase(
                    getContactDetails = GetContactDetailsUseCase(repository),
                    loadContactPhoto = LoadContactPhotoUseCase(repository),
                    getGroups = GetGroupsUseCase(groups),
                    getGroupMembership = GetGroupMembershipUseCase(groups),
                    codec = FakeVCardCodec(),
                ),
            getContactsUseCase = GetContactsUseCase(repository),
            observeSettingsUseCase = ObserveSettingsUseCase(settings),
        )
    }

    @Test
    fun showsContactNames() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna, bob))),
                onAction = { action ->
                    Unit
                },
            )
        }

        compose.onNodeWithText("Anna").assertIsDisplayed()
        compose.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun contactRowShowsExplicitPrimaryPhone() {
        val contact =
            ContactDetails(
                contactId = 3,
                name = "Kate",
                phones =
                    listOf(
                        PhoneNumber("111", PhoneKind.MOBILE),
                        PhoneNumber("222", PhoneKind.WORK, isPrimary = true),
                    ),
            )
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(contact))),
                onAction = { action ->
                    Unit
                },
            )
        }

        compose.onNodeWithText("222", substring = true).assertIsDisplayed()
    }

    @Test
    fun contactRowClickReportsContactId() {
        val clicked = AtomicReference<Long?>(null)
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna, bob))),
                onAction = { action ->
                    when (action) {
                        is ContactsNavigationAction.ContactClicked -> clicked.set(action.contactId)
                        else -> Unit
                    }
                },
            )
        }

        compose.onNodeWithTag("contact_row_2").performClick()

        assertEquals(2L, clicked.get())
    }

    @Test
    fun favoritesChipClickReportsToggle() {
        val toggled = AtomicBoolean(false)
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.FavoritesOnlyToggled -> toggled.set(true)
                        else -> Unit
                    }
                },
            )
        }

        compose.onNodeWithTag("favorites_chip").performClick()

        assertEquals(true, toggled.get())
    }

    @Test
    fun searchFieldFiltersListThroughViewModel() {
        val repository = FakeAndroidRepository(listOf(anna, bob))
        val viewModel = viewModel(repository)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }
        compose.onNodeWithText("Bob").assertIsDisplayed()

        compose.onNodeWithTag("search_field").performTextInput("Anna")

        compose.waitUntil(timeoutMillis = 5_000) {
            val content = viewModel.uiState.value.content
            content is Loadable.Content && content.data.size == 1
        }
        compose.onNodeWithTag("contact_row_1").assertIsDisplayed()
        compose.onNodeWithTag("contact_row_2").assertDoesNotExist()
    }

    @Test
    fun addFabClickReportsAdd() {
        val added = AtomicBoolean(false)
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onAction = { action ->
                    when (action) {
                        is ContactsNavigationAction.AddClicked -> added.set(true)
                        else -> Unit
                    }
                },
            )
        }

        compose.onNodeWithTag("add_contact_fab").performClick()

        assertEquals(true, added.get())
    }

    @Test
    fun longPressStartsSelectionAndConfirmedDeleteRemovesContact() {
        val repository = FakeAndroidRepository(listOf(anna, bob))
        val viewModel = viewModel(repository)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }

        compose.onNodeWithTag("contact_row_1").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selection == setOf(1L)
        }
        compose.onNodeWithTag("selection_top_bar").assertIsDisplayed()

        compose.onNodeWithTag("delete_selected_button").performClick()
        compose.onNodeWithTag("confirm_batch_delete").performClick()
        // Удаление коммитится по истечении окна отмены (~4 c реального времени).
        compose.waitUntil(timeoutMillis = 10_000) {
            repository.deleted.isNotEmpty()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isSelectionMode
        }

        assertEquals(listOf(1L), repository.deleted)
        compose.onNodeWithTag("selection_top_bar").assertDoesNotExist()
    }

    @Test
    fun addSelectedToGroupThroughPicker() {
        val repository = FakeAndroidRepository(listOf(anna, bob))
        val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                groups = uiState.groups,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        is ContactsListAction.GroupSelected ->
                            viewModel.filters.onGroupSelected(
                                action.groupId,
                            )
                        is ContactsListAction.CreateGroup ->
                            scope.launch {
                                viewModel.groups
                                    .onCreateGroup(
                                        action.title,
                                    )
                            }
                        is ContactsListAction.AddSelectedToGroup ->
                            scope.launch {
                                viewModel.groups
                                    .onAddSelectedToGroup(
                                        action.groupIds,
                                    )
                            }
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }

        compose.onNodeWithTag("contact_row_1").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selection == setOf(1L)
        }

        compose.onNodeWithTag("add_to_group_button").performClick()
        compose.onNodeWithTag("group_check_row_Семья").performClick()
        compose.onNodeWithTag("group_picker_confirm").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            groups.addedToGroup.isNotEmpty()
        }

        assertEquals(100L to listOf(1L), groups.addedToGroup.single())
    }

    @Test
    fun deleteShowsUndoSnackbarAndUndoRestoresContact() {
        val repository = FakeAndroidRepository(listOf(anna, bob))
        val viewModel = viewModel(repository)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }

        compose.onNodeWithTag("contact_row_1").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selection == setOf(1L)
        }
        compose.onNodeWithTag("delete_selected_button").performClick()
        compose.onNodeWithTag("confirm_batch_delete").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.pendingDeleteIds == setOf(1L)
        }
        compose.onNodeWithTag("contact_row_1").assertDoesNotExist()
        compose.onNodeWithTag("undo_snackbar").assertIsDisplayed()

        val undoLabel =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(R.string.action_undo)
        compose.onNodeWithText(undoLabel).performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.pendingDeleteIds
                .isEmpty()
        }
        assertEquals(emptyList<Long>(), repository.deleted)
        compose.onNodeWithTag("contact_row_1").assertIsDisplayed()
    }

    @Test
    fun selectAllButtonTogglesVisibleContacts() {
        val repository = FakeAndroidRepository(listOf(anna, bob))
        val viewModel = viewModel(repository)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }

        compose.onNodeWithTag("contact_row_2").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selection == setOf(2L)
        }

        compose.onNodeWithTag("select_all_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selection == setOf(1L, 2L)
        }

        compose.onNodeWithTag("select_all_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selection
                .isEmpty()
        }
        compose.onNodeWithTag("selection_top_bar").assertDoesNotExist()
    }

    @Test
    fun moreMenuShowsImportAndExport() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onAction = { action ->
                    Unit
                },
            )
        }

        compose.onNodeWithTag("more_actions_button").performClick()

        compose.onNodeWithTag("recents_menu_item").assertIsDisplayed()
        compose.onNodeWithTag("import_menu_item").assertIsDisplayed()
        compose.onNodeWithTag("export_menu_item").assertIsDisplayed()
        compose.onNodeWithTag("duplicates_menu_item").assertIsDisplayed()
        compose.onNodeWithTag("settings_menu_item").assertIsDisplayed()
    }

    @Test
    fun quickActionsShowCallAndSmsForContactWithPhone() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onAction = { action ->
                    Unit
                },
            )
        }

        compose.onNodeWithTag("quick_actions_1").performClick()

        compose.onNodeWithTag("quick_call_1").assertIsDisplayed()
        compose.onNodeWithTag("quick_sms_1").assertIsDisplayed()
    }

    @Test
    fun quickCallShowsExistingConfirmationDialog() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onAction = { action ->
                    Unit
                },
            )
        }

        compose.onNodeWithTag("quick_actions_1").performClick()
        compose.onNodeWithTag("quick_call_1").performClick()

        compose.onNodeWithTag("confirm_call_button").assertIsDisplayed()
    }

    @Test
    fun contactWithoutPhoneHasNoCallOrSmsActions() {
        val noPhone = testDetails(3, "Cara")
        compose.setContent {
            val scope = rememberCoroutineScope()
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(noPhone))),
                onAction = { action ->
                    Unit
                },
            )
        }

        compose.onNodeWithTag("quick_actions_3").assertDoesNotExist()
        compose.onNodeWithTag("quick_favorite_3").assertIsDisplayed()
    }

    @Test
    fun quickFavoriteTogglesWithoutOpeningDetails() {
        val repository = FakeAndroidRepository(listOf(anna))
        val viewModel = viewModel(repository)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        is ContactsListAction.ToggleFavorite ->
                            scope.launch {
                                viewModel.favorites
                                    .onToggleFavorite(
                                        action.contactId,
                                    )
                            }
                        is ContactsListAction.ToggleFavoriteSelected ->
                            scope.launch {
                                viewModel
                                    .onToggleSelectedFavorite()
                            }
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }

        compose.onNodeWithTag("quick_favorite_1").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.favorites == setOf(1L)
        }
        compose.onNodeWithTag("quick_favorite_1").assertIsDisplayed()
    }

    @Test
    fun batchFavoriteTogglesAllSelectedContacts() {
        val repository = FakeAndroidRepository(listOf(anna, bob))
        val viewModel = viewModel(repository)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        is ContactsListAction.ToggleFavorite ->
                            scope.launch {
                                viewModel.favorites
                                    .onToggleFavorite(
                                        action.contactId,
                                    )
                            }
                        is ContactsListAction.ToggleFavoriteSelected ->
                            scope.launch {
                                viewModel
                                    .onToggleSelectedFavorite()
                            }
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }

        compose.onNodeWithTag("contact_row_1").performTouchInput { longClick() }
        compose.onNodeWithTag("contact_row_2").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selection == setOf(1L, 2L)
        }

        compose.onNodeWithTag("favorite_selected_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.favorites == setOf(1L, 2L)
        }

        compose.onNodeWithTag("favorite_selected_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.favorites == emptySet<Long>()
        }
    }

    @Test
    fun favoritesArePinnedWithoutFilterAndHiddenWhileSearching() {
        val repository = FakeAndroidRepository(listOf(anna, bob), initialFavorites = setOf(1))
        val viewModel = viewModel(repository)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is ContactsListAction.SearchQueryChanged ->
                            viewModel.filters.onSearchQueryChange(
                                action.query,
                            )
                        is ContactsListAction.FavoritesOnlyToggled ->
                            viewModel.filters
                                .onToggleFavoritesOnly()
                        is ContactsFileAction.Retry -> viewModel.refresh()
                        is ContactsFileAction.Refresh -> viewModel.refresh()
                        is ContactsListAction.ContactSelected ->
                            viewModel.selection.onContactSelected(
                                action.contactId,
                            )
                        is ContactsListAction.ClearSelection -> viewModel.selection.clear()
                        is ContactsListAction.SelectAll ->
                            viewModel.selection.selectAll(
                                action.contactIds,
                            )
                        is ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
                        is ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
                        is ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
                        is ContactsFileAction.ShareSelected ->
                            scope.launch {
                                viewModel.onShareSelected()
                            }
                        is ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
                        is ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
                        is ContactsFileAction.EventHandled -> viewModel.onEventHandled()
                        is ContactsListAction.ToggleFavorite ->
                            scope.launch {
                                viewModel.favorites
                                    .onToggleFavorite(
                                        action.contactId,
                                    )
                            }
                        is ContactsListAction.ToggleFavoriteSelected ->
                            scope.launch {
                                viewModel
                                    .onToggleSelectedFavorite()
                            }
                        else -> Unit
                    }
                },
            )
        }
        viewModel.refresh()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content &&
                viewModel.uiState.value.favorites == setOf(1L)
        }

        compose.onNodeWithTag("favorites_section_header").assertIsDisplayed()

        compose.onNodeWithTag("search_field").performTextInput("Bob")
        compose.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.pinFavorites
        }
        compose.onNodeWithTag("favorites_section_header").assertDoesNotExist()
    }
}
