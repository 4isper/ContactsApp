package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
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
            )
        }

        compose.onNodeWithText("Anna").assertIsDisplayed()
        compose.onNodeWithText("Bob").assertIsDisplayed()
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
            )
        }

        compose.onNodeWithTag("favorites_chip").performClick()

        assertEquals(true, toggled.get())
    }

    @Test
    fun searchFieldFiltersListThroughViewModel() {
        val repository = FakeAndroidRepository(listOf(anna, bob))
        val viewModel =
            ContactsViewModel(
                GetContactsUseCase(repository),
                ObserveFavoritesUseCase(repository),
            )
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
            )
        }

        compose.onNodeWithTag("add_contact_fab").performClick()

        assertEquals(true, added.get())
    }
}
