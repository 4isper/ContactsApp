package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.testDetails
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
    ) = ContactsViewModel(
        GetContactsUseCase(repository),
        GetContactAccountsUseCase(repository),
        ObserveFavoritesUseCase(repository),
        ObserveSettingsUseCase(settings),
        GetContactDetailsUseCase(repository),
        DeleteContactsUseCase(repository, repository),
        ExportContactsUseCase(
            getContactDetails = GetContactDetailsUseCase(repository),
            loadContactPhoto = LoadContactPhotoUseCase(repository),
            getGroups = GetGroupsUseCase(groups),
            getGroupMembership = GetGroupMembershipUseCase(groups),
            codec = FakeVCardCodec(),
        ),
        GetGroupsUseCase(groups),
        GetGroupMembershipUseCase(groups),
        CreateGroupUseCase(groups),
        AddContactsToGroupUseCase(groups),
        ToggleFavoriteUseCase(repository),
        RemoveFavoriteUseCase(repository),
        FakeContactsChangeSource(),
    )

    @Test
    fun showsContactNames() {
        compose.setContent {
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna, bob))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = {},
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
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
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(contact))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = {},
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
            )
        }

        compose.onNodeWithText("222", substring = true).assertIsDisplayed()
    }

    @Test
    fun contactRowClickReportsContactId() {
        val clicked = AtomicReference<Long?>(null)
        compose.setContent {
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna, bob))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = { clicked.set(it) },
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
            )
        }

        compose.onNodeWithTag("contact_row_2").performClick()

        assertEquals(2L, clicked.get())
    }

    @Test
    fun favoritesChipClickReportsToggle() {
        val toggled = AtomicBoolean(false)
        compose.setContent {
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = { toggled.set(true) },
                onContactClick = {},
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
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
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = {},
                onAddClick = { added.set(true) },
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
                groups = uiState.groups,
                onGroupSelected = viewModel::onGroupSelected,
                onCreateGroup = viewModel::onCreateGroup,
                onManageGroupsClick = {},
                onAddSelectedToGroup = viewModel::onAddSelectedToGroup,
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
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
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = {},
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
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
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = {},
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
            )
        }

        compose.onNodeWithTag("quick_actions_1").performClick()

        compose.onNodeWithTag("quick_call_1").assertIsDisplayed()
        compose.onNodeWithTag("quick_sms_1").assertIsDisplayed()
    }

    @Test
    fun quickCallShowsExistingConfirmationDialog() {
        compose.setContent {
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(anna))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = {},
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
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
            ContactList(
                uiState = ContactsUiState(content = Loadable.Content(listOf(noPhone))),
                onSearchQueryChange = {},
                onFavoritesOnlyChange = {},
                onContactClick = {},
                onAddClick = {},
                onRetry = {},
                onRefresh = {},
                onContactSelected = {},
                onClearSelection = {},
                onSelectAll = {},
                onDeleteSelected = {},
                onUndoDelete = {},
                onDeleteWindowClosed = {},
                onShareSelected = {},
                onImportText = {},
                onExportRequested = {},
                onExportSelectionRequested = {},
                onEventHandled = {},
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
                onToggleFavorite = viewModel::onToggleFavorite,
                onToggleFavoriteSelected = viewModel::onToggleSelectedFavorite,
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
                onToggleFavorite = viewModel::onToggleFavorite,
                onToggleFavoriteSelected = viewModel::onToggleSelectedFavorite,
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
            val uiState by viewModel.uiState.collectAsState()
            ContactList(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
                onContactClick = {},
                onAddClick = {},
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onContactSelected = viewModel::onContactSelected,
                onClearSelection = viewModel::onClearSelection,
                onSelectAll = viewModel::onSelectAll,
                onDeleteSelected = viewModel::onDeleteSelected,
                onUndoDelete = viewModel::onUndoDelete,
                onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
                onShareSelected = viewModel::onShareSelected,
                onImportText = {},
                onExportRequested = viewModel.files::onExportRequested,
                onExportSelectionRequested = viewModel::onExportSelection,
                onEventHandled = viewModel::onEventHandled,
                onToggleFavorite = viewModel::onToggleFavorite,
                onToggleFavoriteSelected = viewModel::onToggleSelectedFavorite,
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
