package com.m4isper.contactsapp.presentation.import

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.MergeContactsUseCase
import com.m4isper.contactsapp.domain.usecase.SetContactPhotoUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportPreviewUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun viewModel(
        repository: FakeAndroidRepository,
        codec: FakeVCardCodec,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
    ) = ImportPreviewViewModel(
        codec = codec,
        getContacts = GetContactsUseCase(repository),
        getContactAccounts = GetContactAccountsUseCase(repository),
        importContacts =
            ImportContactsUseCase(
                createContact = CreateContactUseCase(repository),
                mergeContacts = MergeContactsUseCase(repository),
                setContactPhoto = SetContactPhotoUseCase(repository),
                addContactsToGroup = AddContactsToGroupUseCase(groups),
                getGroups = GetGroupsUseCase(groups),
                deleteContacts = DeleteContactsUseCase(repository, repository),
            ),
    )

    private fun form(name: String) = ContactForm(name = EditableName(display = name))

    @Test
    fun showsParsedContactsAndCreatesThem() {
        val repository = FakeAndroidRepository()
        val codec = FakeVCardCodec(forms = listOf(form("Anna"), form("Bob")))
        val viewModel = viewModel(repository, codec)
        compose.setContent {
            ImportPreviewScreen(onBack = {}, onDone = {}, viewModel = viewModel)
        }

        viewModel.prepare("file")
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.entries.size == 2 }

        compose.onNodeWithText("Anna").assertIsDisplayed()
        compose.onNodeWithText("Bob").assertIsDisplayed()
        compose.onNodeWithTag("import_confirm_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.outcome != null }

        assertEquals(listOf("Anna", "Bob"), repository.created.map { it.name.display })
    }

    @Test
    fun skippedContactIsNotCreated() {
        val repository = FakeAndroidRepository()
        val codec = FakeVCardCodec(forms = listOf(form("Anna"), form("Bob")))
        val viewModel = viewModel(repository, codec)
        compose.setContent {
            ImportPreviewScreen(onBack = {}, onDone = {}, viewModel = viewModel)
        }

        viewModel.prepare("file")
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.entries.size == 2 }

        compose.onNodeWithTag("import_decision_skip_0").performClick()
        compose.onNodeWithTag("import_confirm_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.outcome != null }

        assertEquals(listOf("Bob"), repository.created.map { it.name.display })
    }
}
