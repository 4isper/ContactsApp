package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.contacts.components.ContactList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Пустые состояния списка с учётом активных фильтров (issue 39). */
@RunWith(AndroidJUnit4::class)
class ContactsEmptyStateUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        uiState: ContactsUiState,
        onAction: (ContactsUiAction) -> Unit = {},
    ) {
        compose.setContent {
            ContactList(
                uiState = uiState,
                onAction = onAction,
                onCreateContact = {},
            )
        }
    }

    private fun emptyState(
        searchQuery: String = "",
        showFavoritesOnly: Boolean = false,
        groups: List<ContactGroup> = emptyList(),
        selectedGroupId: Long? = null,
        selectedAccount: ContactAccount? = null,
    ) = ContactsUiState(
        searchQuery = searchQuery,
        showFavoritesOnly = showFavoritesOnly,
        content = Loadable.Empty,
        groups = groups,
        selectedGroupId = selectedGroupId,
        selectedAccount = selectedAccount,
    )

    @Test
    fun completelyEmptyBookOffersCreateAndImport() {
        setContent(uiState = emptyState())

        compose.onNodeWithText("No contacts found").assertIsDisplayed()
        compose.onNodeWithText("Create contact").assertIsDisplayed()
        compose.onNodeWithText("Import from file").assertIsDisplayed()
    }

    @Test
    fun groupEmptyNamesGroupAndOffersShowAll() {
        val cleared = AtomicReference<Long?>(Long.MAX_VALUE)
        setContent(
            uiState = emptyState(groups = listOf(ContactGroup(1, "Family")), selectedGroupId = 1),
            onAction = { action ->
                if (action is ContactsListAction.GroupSelected) cleared.set(action.groupId)
            },
        )

        compose.onNodeWithText("No contacts in group “Family”").assertIsDisplayed()
        compose.onNodeWithText("Show all contacts").assertIsDisplayed()
        compose.onNodeWithText("Create contact").assertDoesNotExist()

        compose.onNodeWithText("Show all contacts").performClick()
        compose.runOnIdle { assertEquals(null, cleared.get()) }
    }

    @Test
    fun accountEmptyNamesAccountAndOffersShowAll() {
        val cleared = AtomicBoolean(false)
        setContent(
            uiState = emptyState(selectedAccount = ContactAccount(name = "user@gmail.com")),
            onAction = { action ->
                if (action is ContactsListAction.AccountSelected && action.account == null) {
                    cleared.set(true)
                }
            },
        )

        compose.onNodeWithText("No contacts in account “user@gmail.com”").assertIsDisplayed()
        compose.onNodeWithText("Show all contacts").assertIsDisplayed()

        compose.onNodeWithText("Show all contacts").performClick()
        compose.runOnIdle { assertEquals(true, cleared.get()) }
    }

    @Test
    fun searchEmptyOffersClearSearchInsteadOfCreate() {
        val cleared = AtomicReference<String?>(null)
        setContent(
            uiState = emptyState(searchQuery = "zzz"),
            onAction = { action ->
                if (action is ContactsListAction.SearchQueryChanged) cleared.set(action.query)
            },
        )

        compose.onNodeWithText("Nothing found for “zzz”").assertIsDisplayed()
        compose.onNodeWithText("Clear search").assertIsDisplayed()
        compose.onNodeWithText("Create contact").assertDoesNotExist()

        compose.onNodeWithText("Clear search").performClick()
        compose.runOnIdle { assertEquals("", cleared.get()) }
    }

    @Test
    fun favoritesEmptyOffersShowAll() {
        val toggled = AtomicBoolean(false)
        setContent(
            uiState = emptyState(showFavoritesOnly = true),
            onAction = { action ->
                if (action is ContactsListAction.FavoritesOnlyToggled) toggled.set(true)
            },
        )

        compose.onNodeWithText("No favorite contacts").assertIsDisplayed()
        compose.onNodeWithText("Show all contacts").assertIsDisplayed()

        compose.onNodeWithText("Show all contacts").performClick()
        compose.runOnIdle { assertEquals(true, toggled.get()) }
    }

    @Test
    fun searchWithGroupFilterOffersClearSearchAndClearFilters() {
        val queries = AtomicReference<List<String>>(emptyList())
        val groupCleared = AtomicBoolean(false)
        setContent(
            uiState =
                emptyState(
                    searchQuery = "zzz",
                    groups = listOf(ContactGroup(1, "Family")),
                    selectedGroupId = 1,
                ),
            onAction = { action ->
                if (action is ContactsListAction.SearchQueryChanged) {
                    queries.set(queries.get() + action.query)
                }
                if (action is ContactsListAction.GroupSelected && action.groupId == null) {
                    groupCleared.set(true)
                }
            },
        )

        compose.onNodeWithText("Clear search").assertIsDisplayed()
        compose.onNodeWithText("Clear filters").assertIsDisplayed()
        compose.onNodeWithText("Create contact").assertDoesNotExist()

        compose.onNodeWithText("Clear filters").performClick()
        compose.runOnIdle {
            assertEquals(listOf(""), queries.get())
            assertEquals(true, groupCleared.get())
        }
    }

    @Test
    fun favoritesWithGroupFilterOffersClearAll() {
        val groupCleared = AtomicBoolean(false)
        val favoritesToggled = AtomicBoolean(false)
        setContent(
            uiState =
                emptyState(
                    showFavoritesOnly = true,
                    groups = listOf(ContactGroup(1, "Family")),
                    selectedGroupId = 1,
                ),
            onAction = { action ->
                if (action is ContactsListAction.GroupSelected && action.groupId == null) {
                    groupCleared.set(true)
                }
                if (action is ContactsListAction.FavoritesOnlyToggled) favoritesToggled.set(true)
            },
        )

        compose.onNodeWithText("No contacts match the active filters").assertIsDisplayed()
        compose.onNodeWithText("Clear filters").assertIsDisplayed()

        compose.onNodeWithText("Clear filters").performClick()
        compose.runOnIdle {
            assertEquals(true, groupCleared.get())
            assertEquals(true, favoritesToggled.get())
        }
    }
}
