package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ContactEditorUiTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_CONTACTS)

    private fun viewModel(repository: FakeAndroidRepository) =
        ContactEditorViewModel(
            GetContactFormUseCase(repository),
            CreateContactUseCase(repository),
            UpdateContactUseCase(repository),
            DeleteContactUseCase(repository, repository),
        )

    @Test
    fun createMode_savesValidForm() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_name_field").performTextInput("Anna")
        compose.onAllNodesWithTag("editor_phone_field")[0].performTextInput("111")
        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(true, done.get())
        assertEquals(1, repository.created.size)
        assertEquals("Anna", repository.created.single().name)
        assertEquals(
            listOf("111"),
            repository.created
                .single()
                .phones
                .map { it.number },
        )
    }

    @Test
    fun createMode_emptySaveCreatesNothing() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onNodeWithTag("editor_save_button").performClick()
        compose.waitForIdle()

        assertEquals(false, viewModel.uiState.value.closeScreen)
        assertEquals(false, done.get())
        assertEquals(true, repository.created.isEmpty())
    }

    @Test
    fun addPhoneButton_addsSecondRow() {
        val repository = FakeAndroidRepository()
        val viewModel = viewModel(repository)
        compose.setContent {
            ContactEditorScreen(
                contactId = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel,
            )
        }

        compose.onAllNodesWithTag("editor_phone_field").assertCountEquals(1)
        compose.onNodeWithTag("editor_add_phone_button").performClick()
        compose.onAllNodesWithTag("editor_phone_field").assertCountEquals(2)
    }

    @Test
    fun editMode_prefillsAndUpdates() {
        val repository = FakeAndroidRepository(listOf(testDetails(7, "Anna", "111")))
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = 7,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.name == "Anna"
        }

        compose.onNodeWithTag("editor_save_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(true, done.get())
        assertEquals(1, repository.updated.size)
        assertEquals(7L, repository.updated.single().first)
        assertEquals(
            "Anna",
            repository.updated
                .single()
                .second.name,
        )
    }

    @Test
    fun editMode_deleteRemovesContact() {
        val repository = FakeAndroidRepository(listOf(testDetails(7, "Anna", "111")))
        val viewModel = viewModel(repository)
        val done = AtomicBoolean(false)
        compose.setContent {
            ContactEditorScreen(
                contactId = 7,
                onDone = { done.set(true) },
                onBack = {},
                viewModel = viewModel,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.name == "Anna"
        }

        compose.onNodeWithTag("editor_delete_button").performClick()
        compose.onNodeWithTag("confirm_delete_button").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.closeScreen }
        compose.waitForIdle()
        assertEquals(true, done.get())
        assertEquals(listOf(7L), repository.deleted)
    }
}
