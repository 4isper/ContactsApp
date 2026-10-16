package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableNote
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEditorRepository :
    ContactsRepository,
    FavoritesRepository {
    val created = mutableListOf<ContactForm>()
    val updated = mutableListOf<Pair<Long, ContactForm>>()
    var formToLoad: ContactForm? = null

    override suspend fun getContacts() = emptyList<com.m4isper.contactsapp.domain.model.ContactDetails>()

    override suspend fun getContactDetails(contactId: Long) = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = formToLoad

    override suspend fun createContact(form: ContactForm) {
        created += form
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        updated += contactId to form
    }

    override suspend fun deleteContact(contactId: Long) = Unit

    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet<Long>()).asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContactEditorViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(repository: FakeEditorRepository): ContactEditorViewModel {
        val viewModel =
            ContactEditorViewModel(
                GetContactFormUseCase(repository),
                CreateContactUseCase(repository),
                UpdateContactUseCase(repository),
                DeleteContactUseCase(repository, repository),
            )
        backgroundScope.launch { viewModel.uiState.collect { } }
        return viewModel
    }

    @Test
    fun `save closes screen on JVM`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onNameChange("Anna")
            viewModel.onSave()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.closeScreen)
            assertEquals(1, repository.created.size)
        }

    @Test
    fun `load existing contact fills all fields`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = "Anna",
                            emails = listOf(EditableEmail(dataId = 9, address = "anna@example.com")),
                            organization = EditableOrganization(company = "ACME", title = "CTO"),
                            note = EditableNote(note = "Note"),
                            birthday = EditableBirthday(value = Birthday(day = 24, month = 5, year = 1990)),
                            photoUri = "content://photo/1",
                        )
                }
            val viewModel = viewModel(repository)

            viewModel.load(7)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Anna", state.name)
            assertEquals("anna@example.com", state.emails.single().address)
            assertEquals("ACME", state.organization.company)
            assertEquals("Note", state.note.note)
            assertEquals(Birthday(day = 24, month = 5, year = 1990), state.birthday.value)
            assertEquals("content://photo/1", state.photoUri)
            assertEquals(false, state.isNew)
        }

    @Test
    fun `isDirty ignores blank rows and detects edits`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            assertEquals(false, viewModel.isDirty())

            viewModel.onAddEmail()
            advanceUntilIdle()
            assertEquals(false, viewModel.isDirty())

            val rowId =
                viewModel.uiState.value.emails
                    .single()
                    .rowId
            viewModel.onEmailChange(rowId, "anna@example.com")
            advanceUntilIdle()
            assertEquals(true, viewModel.isDirty())

            viewModel.onRemoveEmail(rowId)
            advanceUntilIdle()
            assertEquals(false, viewModel.isDirty())
        }

    @Test
    fun `save sends full form with photo and birthday`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onNameChange("Anna")
            viewModel.onAddEmail()
            advanceUntilIdle()
            viewModel.onEmailChange(
                viewModel.uiState.value.emails
                    .first()
                    .rowId,
                "anna@example.com",
            )
            viewModel.onAddAddress()
            advanceUntilIdle()
            viewModel.onAddressChange(
                viewModel.uiState.value.addresses
                    .first()
                    .rowId,
                "Moscow",
            )
            viewModel.onAddWebsite()
            advanceUntilIdle()
            viewModel.onWebsiteChange(
                viewModel.uiState.value.websites
                    .first()
                    .rowId,
                "https://example.com",
            )
            viewModel.onOrganizationCompanyChange("ACME")
            viewModel.onNoteChange("Note")
            viewModel.onBirthdayChange(Birthday(day = 24, month = 5, year = 1990))
            viewModel.onPhotoPicked("content://photo/1")
            viewModel.onSave()
            advanceUntilIdle()

            val form = repository.created.single()
            assertEquals("Anna", form.name)
            assertEquals("anna@example.com", form.emails.single().address)
            assertEquals("Moscow", form.addresses.single().address)
            assertEquals("https://example.com", form.websites.single().url)
            assertEquals("ACME", form.organization.company)
            assertEquals("Note", form.note.note)
            assertEquals(Birthday(day = 24, month = 5, year = 1990), form.birthday.value)
            assertEquals("content://photo/1", form.photoUri)
            assertEquals(true, form.photoChanged)
        }

    @Test
    fun `removing photo marks form dirty`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad = ContactForm(name = "Anna", photoUri = "content://photo/1")
                }
            val viewModel = viewModel(repository)
            viewModel.load(7)
            advanceUntilIdle()

            assertEquals(false, viewModel.isDirty())

            viewModel.onPhotoRemoved()
            advanceUntilIdle()
            assertEquals(true, viewModel.isDirty())

            viewModel.onSave()
            advanceUntilIdle()

            val (contactId, form) = repository.updated.single()
            assertEquals(7L, contactId)
            assertEquals(null, form.photoUri)
            assertEquals(true, form.photoChanged)
        }
}
