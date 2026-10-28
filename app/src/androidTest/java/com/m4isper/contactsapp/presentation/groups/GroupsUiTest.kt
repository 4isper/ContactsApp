package com.m4isper.contactsapp.presentation.groups

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.RenameGroupUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun viewModel(repository: FakeGroupsRepository) =
        GroupsViewModel(
            GetGroupsUseCase(repository),
            GetGroupMembershipUseCase(repository),
            CreateGroupUseCase(repository),
            RenameGroupUseCase(repository),
            DeleteGroupUseCase(repository),
        )

    @Test
    fun emptyStateExplainsGroupsAndOpensCreateSheet() {
        val repository = FakeGroupsRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            GroupsScreen(onBack = {}, viewModel = viewModel)
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Empty
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.groups_empty)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.groups_empty_desc)).assertIsDisplayed()

        compose.onNodeWithText(context.getString(R.string.groups_create_action)).performClick()
        compose.onNodeWithTag("group_name_field").performTextInput("Work")
        compose.onNodeWithTag("create_group_confirm").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { repository.groups.isNotEmpty() }
        assertEquals(listOf("Work"), repository.groups.map { it.title })
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.content is Loadable.Content
        }
        compose.onNodeWithText("Work").assertIsDisplayed()
    }
}
