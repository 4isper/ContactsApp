package com.m4isper.contactsapp.presentation.groups

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.MergeGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.RemoveContactsFromGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.RenameGroupUseCase
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Экран группы: состав участников и удаление из группы. */
@RunWith(AndroidJUnit4::class)
class GroupDetailUiTest {
    @get:Rule
    val compose = createComposeRule()

    // Экран запрашивает WRITE_CONTACTS при открытии: выдаём заранее, чтобы
    // системный диалог не перекрыл тестируемое окно.
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_CONTACTS)

    private fun viewModel(
        repository: FakeAndroidRepository,
        groups: FakeGroupsRepository,
    ) = GroupDetailViewModel(
        getGroupsUseCase = GetGroupsUseCase(groups),
        getMembershipUseCase = GetGroupMembershipUseCase(groups),
        getContactsUseCase = GetContactsUseCase(repository),
        addContactsToGroupUseCase = AddContactsToGroupUseCase(groups),
        removeContactsFromGroupUseCase = RemoveContactsFromGroupUseCase(groups),
        renameGroupUseCase = RenameGroupUseCase(groups),
        deleteGroupUseCase = DeleteGroupUseCase(groups),
        createGroupUseCase = CreateGroupUseCase(groups),
        mergeGroupsUseCase = MergeGroupsUseCase(groups),
    )

    @Test
    fun showsMembersAndRemovesThem() {
        val repository = FakeAndroidRepository(listOf(testDetails(7, "Anna", "111")))
        val groups =
            FakeGroupsRepository(
                groups = listOf(ContactGroup(id = 1, title = "Friends")),
                membership = mapOf(7L to setOf(1L)),
            )
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            GroupDetailScreen(
                groupId = 1,
                onBack = {},
                onContactClick = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.members
                .isNotEmpty()
        }

        compose.onNodeWithTag("group_member_7").assertIsDisplayed()
        compose.onNodeWithText("Anna").assertIsDisplayed()

        compose.onNodeWithTag("remove_group_member_7").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { groups.removed.isNotEmpty() }

        assertEquals(listOf(1L to listOf(7L)), groups.removed)
    }

    @Test
    fun picker_excludesMembersKeepsSelectionAcrossSearchAndAddsChosen() {
        val repository =
            FakeAndroidRepository(
                listOf(testDetails(7, "Anna", "111"), testDetails(8, "Bob", "222"), testDetails(9, "Kate", "333")),
            )
        val groups =
            FakeGroupsRepository(
                groups = listOf(ContactGroup(id = 1, title = "Friends")),
                membership = mapOf(7L to setOf(1L)),
            )
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            GroupDetailScreen(
                groupId = 1,
                onBack = {},
                onContactClick = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.members.size > 0 }

        compose.onNodeWithTag("add_group_member_fab").performClick()
        compose.onNodeWithTag("contact_picker_dialog").assertIsDisplayed()
        compose.onNodeWithTag("contact_picker_row_7").assertDoesNotExist()
        compose.onNodeWithTag("contact_picker_row_8").assertIsDisplayed()
        compose.onNodeWithTag("contact_picker_row_9").assertIsDisplayed()

        compose.onNodeWithTag("contact_picker_row_8").performClick()
        compose.onNodeWithTag("contact_picker_confirm").assertTextContains("Add 1 contact")

        compose.onNodeWithTag("search_field").performTextInput("Bob")
        compose.onNodeWithTag("contact_picker_row_8").assertIsDisplayed()
        compose.onNodeWithTag("contact_picker_row_9").assertDoesNotExist()
        compose.onNodeWithContentDescription("Clear search").performClick()

        compose.onNodeWithTag("search_field").performTextInput("Kate")
        compose.onNodeWithTag("contact_picker_row_9").performClick()
        compose.onNodeWithContentDescription("Clear search").performClick()

        compose.onNodeWithTag("contact_picker_confirm").assertTextContains("Add 2 contacts")
        compose.onNodeWithTag("contact_picker_confirm").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { groups.addedToGroup.isNotEmpty() }

        val added = groups.addedToGroup.single()
        assertEquals(1L, added.first)
        assertEquals(setOf(8L, 9L), added.second.toSet())
    }

    @Test
    fun picker_searchEmpty_showsNoResultsWithClearAction() {
        val repository = FakeAndroidRepository(listOf(testDetails(8, "Bob", "222")))
        val groups =
            FakeGroupsRepository(
                groups = listOf(ContactGroup(id = 1, title = "Friends")),
                membership = emptyMap(),
            )
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            GroupDetailScreen(
                groupId = 1,
                onBack = {},
                onContactClick = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.group != null }

        compose.onNodeWithTag("add_group_member_fab").performClick()
        compose.onNodeWithTag("search_field").performTextInput("zzz")

        compose.onNodeWithText("Nothing found for “zzz”").assertIsDisplayed()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithTag("contact_picker_row_8").assertIsDisplayed()
    }

    @Test
    fun picker_allMembers_hidesAddFab() {
        val repository = FakeAndroidRepository(listOf(testDetails(7, "Anna", "111")))
        val groups =
            FakeGroupsRepository(
                groups = listOf(ContactGroup(id = 1, title = "Friends")),
                membership = mapOf(7L to setOf(1L)),
            )
        val viewModel = viewModel(repository, groups)
        compose.setContent {
            GroupDetailScreen(
                groupId = 1,
                onBack = {},
                onContactClick = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.members.size > 0 }

        compose.onNodeWithTag("add_group_member_fab").assertDoesNotExist()
    }
}
