package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditableNickname
import com.m4isper.contactsapp.domain.model.EditableNote
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
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
                DeleteContactUseCase(DeleteContactsUseCase(repository, repository)),
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

            viewModel.onDisplayNameChange("Anna")
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
                            name = EditableName(display = "Anna"),
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
            assertEquals(NameMode.DISPLAY, state.nameMode)
            assertEquals("Anna", state.displayName)
            val email = state.emails.single()
            assertEquals("anna@example.com", email.value.address)
            assertEquals("ACME", state.organization.company)
            assertEquals("Note", state.note.note)
            assertEquals(Birthday(day = 24, month = 5, year = 1990), state.birthday.value)
            assertEquals("content://photo/1", state.photoUri)
            assertEquals(false, state.isNew)
        }

    @Test
    fun `load existing contact fills instant messages`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            instantMessages =
                                listOf(
                                    EditableInstantMessage(
                                        dataId = 3,
                                        address = "skype_anna",
                                        protocol = ImProtocol.SKYPE,
                                    ),
                                ),
                        )
                }
            val viewModel = viewModel(repository)

            viewModel.load(7)
            advanceUntilIdle()

            val row =
                viewModel.uiState.value.instantMessages
                    .single()
            assertEquals(3L, row.value.dataId)
            assertEquals("skype_anna", row.value.address)
            assertEquals(ImProtocol.SKYPE, row.value.protocol)
        }

    @Test
    fun `save sends custom instant message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onAddIm()
            advanceUntilIdle()
            val rowId =
                viewModel.uiState.value.instantMessages
                    .single()
                    .rowId
            viewModel.onImChange(rowId, "tg_anna")
            viewModel.onImCustomProtocolChange(rowId, "Telegram")
            viewModel.onSave()
            advanceUntilIdle()

            val im =
                repository.created
                    .single()
                    .instantMessages
                    .single()
            assertEquals("tg_anna", im.address)
            assertEquals(ImProtocol.CUSTOM, im.protocol)
            assertEquals("Telegram", im.customProtocol)
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

            viewModel.onAddIm()
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

            viewModel.onDisplayNameChange("Anna")
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
            assertEquals("Anna", form.name.display)
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
                    formToLoad = ContactForm(name = EditableName(display = "Anna"), photoUri = "content://photo/1")
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

    @Test
    fun `load structured name opens structured mode`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name =
                                EditableName(
                                    mode = NameMode.STRUCTURED,
                                    display = "Иван Петрович Петров",
                                    given = "Иван",
                                    family = "Петров",
                                    middle = "Петрович",
                                ),
                        )
                }
            val viewModel = viewModel(repository)

            viewModel.load(7)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(NameMode.STRUCTURED, state.nameMode)
            assertEquals("Иван", state.givenName)
            assertEquals("Петров", state.familyName)
            assertEquals("Петрович", state.middleName)
            assertEquals("Иван Петрович Петров", state.effectiveName)
        }

    @Test
    fun `toggle to structured prefills given with display name`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onDisplayNameChange("Иван Петров")
            viewModel.onToggleNameMode()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(NameMode.STRUCTURED, state.nameMode)
            assertEquals("Иван Петров", state.givenName)
        }

    @Test
    fun `toggle to display composes name from components`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onToggleNameMode()
            viewModel.onGivenNameChange("Иван")
            viewModel.onMiddleNameChange("Петрович")
            viewModel.onFamilyNameChange("Петров")
            viewModel.onToggleNameMode()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(NameMode.DISPLAY, state.nameMode)
            assertEquals("Иван Петрович Петров", state.displayName)
        }

    @Test
    fun `save structured name sends components`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onToggleNameMode()
            viewModel.onGivenNameChange("Иван")
            viewModel.onFamilyNameChange("Петров")
            viewModel.onSave()
            advanceUntilIdle()

            val name = repository.created.single().name
            assertEquals(NameMode.STRUCTURED, name.mode)
            assertEquals("Иван", name.given)
            assertEquals("Петров", name.family)
        }

    @Test
    fun `load phonetic name fields`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name =
                                EditableName(
                                    mode = NameMode.STRUCTURED,
                                    given = "Иван",
                                    phoneticGiven = "Ivan",
                                    phoneticMiddle = "Petrovich",
                                    phoneticFamily = "Petrov",
                                ),
                        )
                }
            val viewModel = viewModel(repository)

            viewModel.load(7)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Ivan", state.phoneticGivenName)
            assertEquals("Petrovich", state.phoneticMiddleName)
            assertEquals("Petrov", state.phoneticFamilyName)
        }

    @Test
    fun `save phonetic fields sends them`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onToggleNameMode()
            viewModel.onGivenNameChange("Иван")
            viewModel.onPhoneticGivenNameChange("Ivan")
            viewModel.onPhoneticFamilyNameChange("Petrov")
            viewModel.onSave()
            advanceUntilIdle()

            val name = repository.created.single().name
            assertEquals("Ivan", name.phoneticGiven)
            assertEquals("Petrov", name.phoneticFamily)
        }

    @Test
    fun `mode toggle marks form dirty and back`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            assertEquals(false, viewModel.isDirty())

            viewModel.onToggleNameMode()
            advanceUntilIdle()
            assertEquals(true, viewModel.isDirty())

            viewModel.onToggleNameMode()
            advanceUntilIdle()
            assertEquals(false, viewModel.isDirty())
        }

    @Test
    fun `load existing contact fills nickname relations and events`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            nickname = EditableNickname(nickname = "Нюша"),
                            relations =
                                listOf(
                                    EditableRelation(dataId = 5, name = "Bob", kind = RelationKind.FRIEND),
                                    EditableRelation(
                                        name = "Михалыч",
                                        kind = RelationKind.OTHER,
                                        customLabel = "Кум",
                                    ),
                                ),
                            events =
                                listOf(
                                    EditableEvent(
                                        dataId = 6,
                                        date = Birthday(day = 1, month = 6, year = 2015),
                                        kind = EventKind.ANNIVERSARY,
                                    ),
                                ),
                        )
                }
            val viewModel = viewModel(repository)

            viewModel.load(7)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Нюша", state.nickname.nickname)
            val relations = state.relations
            assertEquals(listOf("Bob", "Михалыч"), relations.map { it.value.name })
            val bob = relations[0]
            assertEquals(5L, bob.value.dataId)
            assertEquals(RelationKind.FRIEND, bob.value.kind)
            assertEquals("Кум", relations[1].value.customLabel)
            val event = state.events.single()
            assertEquals(6L, event.value.dataId)
            assertEquals(Birthday(day = 1, month = 6, year = 2015), event.value.date)
            assertEquals(EventKind.ANNIVERSARY, event.value.kind)
        }

    @Test
    fun `save sends nickname relations and events`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onDisplayNameChange("Anna")
            viewModel.onNicknameChange("Нюша")
            viewModel.onAddRelation()
            advanceUntilIdle()
            val relationRowId =
                viewModel.uiState.value.relations
                    .single()
                    .rowId
            viewModel.onRelationNameChange(relationRowId, "Bob")
            viewModel.onRelationKindChange(relationRowId, RelationKind.SISTER)
            viewModel.onAddEvent()
            advanceUntilIdle()
            val eventRowId =
                viewModel.uiState.value.events
                    .single()
                    .rowId
            viewModel.onEventDateChange(eventRowId, Birthday(day = 10, month = 9, year = null))
            viewModel.onEventKindChange(eventRowId, EventKind.ANNIVERSARY)
            viewModel.onSave()
            advanceUntilIdle()

            val form = repository.created.single()
            assertEquals("Нюша", form.nickname.nickname)
            assertEquals("Bob", form.relations.single().name)
            assertEquals(RelationKind.SISTER, form.relations.single().kind)
            assertEquals(Birthday(day = 10, month = 9, year = null), form.events.single().date)
            assertEquals(EventKind.ANNIVERSARY, form.events.single().kind)
        }

    @Test
    fun `dirty ignores blank relations and events without date`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onAddRelation()
            viewModel.onAddEvent()
            advanceUntilIdle()
            assertEquals(false, viewModel.isDirty())

            val relationRowId =
                viewModel.uiState.value.relations
                    .single()
                    .rowId
            viewModel.onRelationNameChange(relationRowId, "Bob")
            advanceUntilIdle()
            assertEquals(true, viewModel.isDirty())

            viewModel.onRemoveRelation(relationRowId)
            advanceUntilIdle()
            assertEquals(false, viewModel.isDirty())

            val eventRowId =
                viewModel.uiState.value.events
                    .single()
                    .rowId
            viewModel.onEventDateChange(eventRowId, Birthday(day = 10, month = 9, year = null))
            advanceUntilIdle()
            assertEquals(true, viewModel.isDirty())

            viewModel.onRemoveEvent(eventRowId)
            advanceUntilIdle()
            assertEquals(false, viewModel.isDirty())
        }
}
