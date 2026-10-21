package com.m4isper.contactsapp.presentation.recents

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.testDetails
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentsUiTest {
    @get:Rule(order = 0)
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun viewModel(repository: FakeAndroidRepository) =
        RecentsViewModel(
            GetContactsUseCase(repository),
            ObserveFavoritesUseCase(repository),
            ToggleFavoriteUseCase(repository),
        )

    @Test
    fun showsRecentlyContactedContacts() {
        val repository =
            FakeAndroidRepository(
                listOf(
                    testDetails(1, "Anna", "111").copy(lastContacted = 200, timesContacted = 2),
                    testDetails(2, "Bob", "222").copy(lastContacted = 100, timesContacted = 1),
                    testDetails(3, "Cara", "333"),
                ),
            )
        val viewModel = viewModel(repository)

        compose.setContent {
            RecentsScreen(
                onBack = {},
                onContactClick = {},
                viewModel = viewModel,
            )
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }

        compose.onNodeWithTag("recents_list").assertIsDisplayed()
        compose.onNodeWithText("Anna").assertIsDisplayed()
        compose.onNodeWithText("Bob").assertIsDisplayed()
        compose.onNodeWithText("Cara").assertDoesNotExist()
    }

    @Test
    fun showsEmptyStateWithoutRecentContacts() {
        val viewModel = viewModel(FakeAndroidRepository(listOf(testDetails(1, "Anna", "111"))))

        compose.setContent {
            RecentsScreen(
                onBack = {},
                onContactClick = {},
                viewModel = viewModel,
            )
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Empty
        }

        val emptyText =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(R.string.recents_empty)
        compose.onNodeWithText(emptyText).assertIsDisplayed()
    }
}
