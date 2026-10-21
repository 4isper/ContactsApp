package com.m4isper.contactsapp.presentation.duplicates

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.MergeContactsUseCase
import com.m4isper.contactsapp.domain.usecase.UnmergeContactsUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DuplicatesUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun repository() =
        FakeAndroidRepository(
            listOf(
                testDetails(7, "Anna", "111222"),
                testDetails(8, "Anna", "111222"),
            ),
        )

    private fun viewModel(repository: FakeAndroidRepository) =
        DuplicatesViewModel(
            GetContactsUseCase(repository),
            MergeContactsUseCase(repository),
            UnmergeContactsUseCase(repository),
        )

    private fun awaitContent(viewModel: DuplicatesViewModel) {
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }
    }

    @Test
    fun showsDuplicateGroupWithMembers() {
        val viewModel = viewModel(repository())
        compose.setContent {
            DuplicatesScreen(onBack = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("duplicate_group_7").assertIsDisplayed()
        compose.onNodeWithTag("duplicate_member_7").assertIsDisplayed()
        compose.onNodeWithTag("duplicate_member_8").assertIsDisplayed()
    }

    @Test
    fun mergesIntoSelectedPrimary() {
        val repository = repository()
        val viewModel = viewModel(repository)
        compose.setContent {
            DuplicatesScreen(onBack = {}, viewModel = viewModel)
        }
        awaitContent(viewModel)

        compose.onNodeWithTag("duplicate_member_8").performClick()
        compose.onNodeWithTag("merge_group_7").performClick()

        // Сначала предпросмотр: показываем, что добавится к основному контакту.
        compose.onNodeWithTag("merge_preview_dialog").assertIsDisplayed()
        compose.onNodeWithTag("merge_preview_confirm").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { repository.merged.isNotEmpty() }
        assertEquals(listOf(8L to listOf(7L)), repository.merged)
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Empty
        }
    }
}
