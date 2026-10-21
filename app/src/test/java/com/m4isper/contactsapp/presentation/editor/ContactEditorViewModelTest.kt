package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.EditableAddress
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditableNickname
import com.m4isper.contactsapp.domain.model.EditableNote
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.model.SipKind
import com.m4isper.contactsapp.domain.model.WebsiteKind
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.GetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.SetLastContactAccountUseCase
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
    val createdAccounts = mutableListOf<ContactAccount>()
    var accounts: List<ContactAccount> = listOf(ContactAccount.Local)
    var accountToLoad: ContactAccount = ContactAccount.Local
    val updated = mutableListOf<Pair<Long, ContactForm>>()
    val deleted = mutableListOf<Long>()
    var deleteFails = false
    var formToLoad: ContactForm? = null

    override suspend fun getContacts() = emptyList<com.m4isper.contactsapp.domain.model.ContactDetails>()

    override suspend fun getContactDetails(contactId: Long) = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = formToLoad

    override suspend fun getContactAccount(contactId: Long): ContactAccount = accountToLoad

    override suspend fun getContactAccounts(): List<ContactAccount> = accounts

    override suspend fun createContact(form: ContactForm): Long = createContact(form, ContactAccount.Local)

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long {
        created += form
        createdAccounts += account
        return created.size.toLong()
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        updated += contactId to form
    }

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun copyContact(
        contactId: Long,
        account: com.m4isper.contactsapp.domain.model.ContactAccount,
    ): Long = 1L

    override suspend fun moveContact(
        contactId: Long,
        account: com.m4isper.contactsapp.domain.model.ContactAccount,
    ): Long = 1L

    override suspend fun deleteContact(contactId: Long) {
        deleted += contactId
        if (deleteFails) error("delete failed")
    }

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.ContactLink? = null

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

    private fun TestScope.viewModel(
        repository: FakeEditorRepository,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ): ContactEditorViewModel {
        val viewModel =
            ContactEditorViewModel(
                GetContactFormUseCase(repository),
                CreateContactUseCase(repository),
                UpdateContactUseCase(repository),
                DeleteContactsUseCase(repository, repository),
                GetGroupsUseCase(groups),
                CreateGroupUseCase(groups),
                GetContactAccountsUseCase(repository),
                GetContactAccountUseCase(repository),
                GetDefaultContactAccountUseCase(settings),
                SetLastContactAccountUseCase(settings),
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
    fun `new contact starts from the default account`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val repository =
                FakeEditorRepository().apply {
                    accounts = listOf(ContactAccount.Local, google)
                }
            val settings = FakeSettingsRepository(AppSettings(defaultContactAccount = google))
            val viewModel = viewModel(repository, settings = settings)

            viewModel.load(null)
            advanceUntilIdle()

            assertEquals(google, viewModel.uiState.value.selectedAccount)
        }

    @Test
    fun `new contact falls back to local when last account is unavailable`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val settings = FakeSettingsRepository(AppSettings(defaultContactAccount = google))
            val viewModel = viewModel(FakeEditorRepository(), settings = settings)

            viewModel.load(null)
            advanceUntilIdle()

            assertEquals(ContactAccount.Local, viewModel.uiState.value.selectedAccount)
        }

    @Test
    fun `selecting account remembers it for the next contact`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val repository =
                FakeEditorRepository().apply {
                    accounts = listOf(ContactAccount.Local, google)
                }
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(repository, settings = settings)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onAccountSelected(google)
            advanceUntilIdle()

            assertEquals(listOf(google), settings.lastContactAccounts)
        }

    @Test
    fun `new contact loads accounts and saves to the selected one`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val repository =
                FakeEditorRepository().apply {
                    accounts = listOf(ContactAccount.Local, google)
                }
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            assertEquals(listOf(ContactAccount.Local, google), viewModel.uiState.value.accounts)
            assertEquals(ContactAccount.Local, viewModel.uiState.value.selectedAccount)

            viewModel.onAccountSelected(google)
            viewModel.onDisplayNameChange("Anna")
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(listOf(google), repository.createdAccounts)
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
                            emails = listOf(EditableEmail(rowId = 9, address = "anna@example.com")),
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
                                        rowId = 3,
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
            assertEquals(3L, row.value.rowId)
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
    fun `saves structured fields extended kinds sip and primary values`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel = viewModel(repository)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onDisplayNameChange("Anna")
            viewModel.onAddPhone()
            viewModel.onAddPhone()
            advanceUntilIdle()
            val firstPhone = viewModel.uiState.value.phones[0]
            val secondPhone = viewModel.uiState.value.phones[1]
            viewModel.onPhoneChange(firstPhone.rowId, "111")
            viewModel.onPhoneChange(secondPhone.rowId, "222")
            viewModel.onKindChange(secondPhone.rowId, PhoneKind.WORK_MOBILE)
            viewModel.onMovePhone(secondPhone.rowId, -1)
            viewModel.onSetPrimaryPhone(secondPhone.rowId)

            viewModel.onAddEmail()
            advanceUntilIdle()
            val email = viewModel.uiState.value.emails[0]
            viewModel.onEmailChange(email.rowId, "anna@example.com")
            viewModel.onEmailKindChange(email.rowId, EmailKind.MOBILE)
            viewModel.onSetPrimaryEmail(email.rowId)

            viewModel.onAddAddress()
            advanceUntilIdle()
            val address = viewModel.uiState.value.addresses[0]
            viewModel.onAddressStreetChange(address.rowId, "Lenina 1")
            viewModel.onAddressCityChange(address.rowId, "Moscow")
            viewModel.onAddressRegionChange(address.rowId, "MO")
            viewModel.onAddressPostcodeChange(address.rowId, "101000")
            viewModel.onAddressCountryChange(address.rowId, "Russia")
            viewModel.onAddressKindChange(address.rowId, AddressKind.WORK)

            viewModel.onAddSip()
            advanceUntilIdle()
            val sip = viewModel.uiState.value.sipAddresses[0]
            viewModel.onSipChange(sip.rowId, "sip:anna@example.com")
            viewModel.onSipKindChange(sip.rowId, SipKind.WORK)

            viewModel.onAddWebsite()
            advanceUntilIdle()
            val website = viewModel.uiState.value.websites[0]
            viewModel.onWebsiteChange(website.rowId, "https://blog.example")
            viewModel.onWebsiteKindChange(website.rowId, WebsiteKind.BLOG)

            viewModel.onOrganizationCompanyChange("ACME")
            viewModel.onOrganizationDepartmentChange("Platform")
            viewModel.onOrganizationJobDescriptionChange("Leads engineering")
            viewModel.onOrganizationOfficeLocationChange("London")
            viewModel.onSave()
            advanceUntilIdle()

            val form = repository.created.single()
            assertEquals(listOf("222", "111"), form.phones.map { it.number })
            assertEquals(PhoneKind.WORK_MOBILE, form.phones.first().kind)
            assertEquals(listOf(true, false), form.phones.map { it.isPrimary })
            assertEquals(EmailKind.MOBILE, form.emails.single().kind)
            assertEquals(true, form.emails.single().isPrimary)
            assertEquals(
                EditableAddress(
                    rowId = address.rowId,
                    address = "",
                    kind = AddressKind.WORK,
                    street = "Lenina 1",
                    city = "Moscow",
                    region = "MO",
                    postcode = "101000",
                    country = "Russia",
                ).copy(rowId = form.addresses.single().rowId),
                form.addresses.single(),
            )
            assertEquals("sip:anna@example.com", form.sipAddresses.single().address)
            assertEquals(SipKind.WORK, form.sipAddresses.single().kind)
            assertEquals(WebsiteKind.BLOG, form.websites.single().kind)
            assertEquals("Platform", form.organization.department)
            assertEquals("Leads engineering", form.organization.jobDescription)
            assertEquals("London", form.organization.officeLocation)
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
                                    EditableRelation(rowId = 5, name = "Bob", kind = RelationKind.FRIEND),
                                    EditableRelation(
                                        name = "Михалыч",
                                        kind = RelationKind.OTHER,
                                        customLabel = "Кум",
                                    ),
                                ),
                            events =
                                listOf(
                                    EditableEvent(
                                        rowId = 6,
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
            assertEquals(5L, bob.value.rowId)
            assertEquals(RelationKind.FRIEND, bob.value.kind)
            assertEquals("Кум", relations[1].value.customLabel)
            val event = state.events.single()
            assertEquals(6L, event.value.rowId)
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

    @Test
    fun `untouched unparsed event keeps form not dirty and survives save`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            events =
                                listOf(
                                    EditableEvent(
                                        rowId = 6,
                                        kind = EventKind.OTHER,
                                        unparsedDate = "1985-03-0X",
                                    ),
                                ),
                        )
                }
            val viewModel = viewModel(repository)
            viewModel.load(7)
            advanceUntilIdle()

            val event =
                viewModel.uiState.value.events
                    .single()
                    .value
            assertEquals("1985-03-0X", event.unparsedDate)
            assertEquals(false, viewModel.isDirty())

            viewModel.onSave()
            advanceUntilIdle()

            val form = repository.updated.single().second
            assertEquals("1985-03-0X", form.events.single().unparsedDate)
            assertEquals(null, form.events.single().date)
        }

    @Test
    fun `removing unparsed event makes form dirty`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            events =
                                listOf(
                                    EditableEvent(
                                        rowId = 6,
                                        kind = EventKind.OTHER,
                                        unparsedDate = "1985-03-0X",
                                    ),
                                ),
                        )
                }
            val viewModel = viewModel(repository)
            viewModel.load(7)
            advanceUntilIdle()

            val rowId =
                viewModel.uiState.value.events
                    .single()
                    .rowId
            viewModel.onRemoveEvent(rowId)
            advanceUntilIdle()

            assertEquals(true, viewModel.isDirty())
        }

    @Test
    fun `repeated load of same contact keeps unsaved edits`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad = ContactForm(name = EditableName(display = "Anna"))
                }
            val viewModel = viewModel(repository)
            viewModel.load(7)
            advanceUntilIdle()

            viewModel.onDisplayNameChange("Edited")
            // Повторный вызов — например, после возврата с экрана системных разрешений.
            viewModel.load(7)
            advanceUntilIdle()

            assertEquals("Edited", viewModel.uiState.value.displayName)
        }

    @Test
    fun `load new contact loads groups for picker`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(FakeEditorRepository(), groups)

            viewModel.load(null)
            advanceUntilIdle()

            assertEquals(listOf(ContactGroup(id = 100, title = "Семья")), viewModel.uiState.value.allGroups)
        }

    @Test
    fun `new contact shows only groups of the selected account`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val localGroup = ContactGroup(id = 100, title = "Семья")
            val googleGroup =
                ContactGroup(
                    id = 200,
                    title = "Работа",
                    accountName = google.name,
                    accountType = google.type,
                )
            val repository =
                FakeEditorRepository().apply {
                    accounts = listOf(ContactAccount.Local, google)
                }
            val groups = FakeGroupsRepository(groups = listOf(localGroup, googleGroup))
            val viewModel = viewModel(repository, groups)
            viewModel.load(null)
            advanceUntilIdle()

            assertEquals(listOf(localGroup), viewModel.uiState.value.availableGroups)

            viewModel.onAccountSelected(google)

            assertEquals(listOf(googleGroup), viewModel.uiState.value.availableGroups)
            assertEquals(listOf(localGroup, googleGroup), viewModel.uiState.value.allGroups)
        }

    @Test
    fun `switching account drops selected groups from another account`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            ContactGroup(id = 100, title = "Семья"),
                            ContactGroup(
                                id = 200,
                                title = "Работа",
                                accountName = google.name,
                                accountType = google.type,
                            ),
                        ),
                )
            val repository =
                FakeEditorRepository().apply {
                    accounts = listOf(ContactAccount.Local, google)
                }
            val viewModel = viewModel(repository, groups)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onGroupToggled(100)
            assertEquals(setOf(100L), viewModel.uiState.value.selectedGroupIds)

            viewModel.onAccountSelected(google)
            assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedGroupIds)

            viewModel.onGroupToggled(200)
            viewModel.onAccountSelected(ContactAccount.Local)
            assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedGroupIds)
        }

    @Test
    fun `create group from picker uses the selected account`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val repository =
                FakeEditorRepository().apply {
                    accounts = listOf(ContactAccount.Local, google)
                }
            val groups = FakeGroupsRepository()
            val viewModel = viewModel(repository, groups)
            viewModel.load(null)
            advanceUntilIdle()
            viewModel.onAccountSelected(google)

            viewModel.onCreateGroup("Работа")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(google), groups.createdGroupAccounts)
            assertEquals(
                listOf(
                    ContactGroup(
                        id = 1,
                        title = "Работа",
                        accountName = google.name,
                        accountType = google.type,
                    ),
                ),
                state.allGroups,
            )
            assertEquals(setOf(1L), state.selectedGroupIds)
            assertEquals(state.allGroups, state.availableGroups)
        }

    @Test
    fun `load existing contact preselects its groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            groups = listOf(200L, 100L),
                        )
                }
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            ContactGroup(id = 100, title = "Семья"),
                            ContactGroup(id = 200, title = "Работа"),
                        ),
                )
            val viewModel = viewModel(repository, groups)

            viewModel.load(7)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(setOf(100L, 200L), state.selectedGroupIds)
            // selectedGroups отдаёт метки в порядке списка всех групп.
            assertEquals(listOf("Семья", "Работа"), state.selectedGroups.map { it.title })
        }

    @Test
    fun `existing contact uses its account for groups and new groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")
            val localGroup = ContactGroup(id = 100, title = "Семья")
            val googleGroup =
                ContactGroup(
                    id = 200,
                    title = "Работа",
                    accountName = google.name,
                    accountType = google.type,
                )
            val repository =
                FakeEditorRepository().apply {
                    accountToLoad = google
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            groups = listOf(200L),
                        )
                }
            val groups = FakeGroupsRepository(groups = listOf(localGroup, googleGroup))
            val viewModel = viewModel(repository, groups)
            viewModel.load(7)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(google, state.selectedAccount)
            assertEquals(listOf(googleGroup), state.availableGroups)
            assertEquals(setOf(200L), state.selectedGroupIds)

            viewModel.onCreateGroup("Проект")
            advanceUntilIdle()

            assertEquals(listOf(google), groups.createdGroupAccounts)
            assertEquals(listOf(googleGroup, groups.groups.last()), viewModel.uiState.value.availableGroups)
        }

    @Test
    fun `save sends selected group ids`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onDisplayNameChange("Anna")
            viewModel.onGroupToggled(100)
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(listOf(100L), repository.created.single().groups)
        }

    @Test
    fun `toggling group off removes it from saved form`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            groups = listOf(100L),
                        )
                }
            val groups = FakeGroupsRepository(groups = listOf(ContactGroup(id = 100, title = "Семья")))
            val viewModel = viewModel(repository, groups)
            viewModel.load(7)
            advanceUntilIdle()

            viewModel.onGroupToggled(100)
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(
                emptyList<Long>(),
                repository.updated
                    .single()
                    .second.groups,
            )
        }

    @Test
    fun `toggle on and off keeps form clean`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad =
                        ContactForm(
                            name = EditableName(display = "Anna"),
                            groups = listOf(200L, 100L),
                        )
                }
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            ContactGroup(id = 100, title = "Семья"),
                            ContactGroup(id = 200, title = "Работа"),
                        ),
                )
            val viewModel = viewModel(repository, groups)
            viewModel.load(7)
            advanceUntilIdle()
            assertEquals(false, viewModel.isDirty())

            viewModel.onGroupToggled(300)
            viewModel.onGroupToggled(300)

            assertEquals(false, viewModel.isDirty())
        }

    @Test
    fun `create group from picker adds and selects it`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups = FakeGroupsRepository()
            val viewModel = viewModel(FakeEditorRepository(), groups)
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onCreateGroup("Друзья")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(ContactGroup(id = 1, title = "Друзья")), state.allGroups)
            assertEquals(setOf(1L), state.selectedGroupIds)
        }

    @Test
    fun `delete success closes screen`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad = ContactForm(name = EditableName(display = "Anna"))
                }
            val viewModel = viewModel(repository)
            viewModel.load(7)
            advanceUntilIdle()

            viewModel.onDelete()
            advanceUntilIdle()

            assertEquals(listOf(7L), repository.deleted)
            assertEquals(true, viewModel.uiState.value.closeScreen)
            assertEquals(false, viewModel.uiState.value.deleteFailed)
        }

    @Test
    fun `delete failure keeps screen open and reports it`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeEditorRepository().apply {
                    formToLoad = ContactForm(name = EditableName(display = "Anna"))
                    deleteFails = true
                }
            val viewModel = viewModel(repository)
            viewModel.load(7)
            advanceUntilIdle()

            viewModel.onDelete()
            advanceUntilIdle()

            assertEquals(listOf(7L), repository.deleted)
            assertEquals(false, viewModel.uiState.value.closeScreen)
            assertEquals(true, viewModel.uiState.value.deleteFailed)
        }
}
