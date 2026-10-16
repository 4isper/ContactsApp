package com.m4isper.contactsapp.presentation.details

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ContactDetailsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun viewModel(repository: FakeAndroidRepository) =
        ContactDetailsViewModel(
            GetContactDetailsUseCase(repository),
            ToggleFavoriteUseCase(repository),
            ObserveFavoritesUseCase(repository),
        )

    private fun awaitContent(viewModel: ContactDetailsViewModel) {
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }
    }

    @Test
    fun showsNameAndNumbers() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111", "112"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        // Имя дублируется в топ-баре и шапке — проверяем оба узла.
        compose.onAllNodesWithText("Anna").assertCountEquals(2)
        compose.onNodeWithText("111").assertIsDisplayed()
        compose.onNodeWithText("112").assertIsDisplayed()
    }

    @Test
    fun showsNotFoundForUnknownContact() {
        val viewModel = viewModel(FakeAndroidRepository())
        compose.setContent {
            ContactDetailsScreen(contactId = 42, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Empty
        }

        compose.onNodeWithText("Anna").assertDoesNotExist()
    }

    @Test
    fun phoneTapOpensConfirmDialogAndDismissClosesIt() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("phone_row_111").performClick()
        compose.onNodeWithTag("confirm_call_button").assertIsDisplayed()

        compose.onNodeWithTag("dismiss_call_button").performClick()
        compose.onNodeWithTag("confirm_call_button").assertDoesNotExist()
    }

    @Test
    fun starTogglesFavoriteState() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = {}, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)
        assertEquals(false, viewModel.uiState.value.isFavorite)

        compose.onNodeWithTag("favorite_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isFavorite
        }

        compose.onNodeWithTag("favorite_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.isFavorite
        }
    }

    @Test
    fun backButtonReportsOnBack() {
        val backed = AtomicBoolean(false)
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(contactId = 7, onBack = { backed.set(true) }, onEdit = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("back_button").performClick()

        assertEquals(true, backed.get())
    }

    @Test
    fun editButtonReportsOnEdit() {
        val edited = AtomicBoolean(false)
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(7, "Anna", "111"))))
        compose.setContent {
            ContactDetailsScreen(
                contactId = 7,
                onBack = {},
                onEdit = { edited.set(true) },
                viewModel = viewModel,
            )
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("edit_button").performClick()

        assertEquals(true, edited.get())
    }
}
