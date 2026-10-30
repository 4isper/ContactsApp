package com.m4isper.contactsapp.presentation.import

import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactLink
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.ImportDecision
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.contacts.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ImportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.MergeContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.SetContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class PreviewContactsRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var createDelayMillis: Long = 0,
) : ContactsReader,
    ContactsAccounts,
    ContactsWriter,
    ContactsMerger,
    FavoritesRepository {
    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = emptyList()

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun getContactAccount(contactId: Long): ContactAccount = ContactAccount.Local

    override suspend fun unmergeContacts(link: ContactLink) = Unit

    val created = mutableListOf<ContactForm>()
    private val favorites = MutableStateFlow(emptySet<Long>())

    override suspend fun getContacts(): List<ContactDetails> = contacts

    override suspend fun getContactDetails(contactId: Long): ContactDetails? =
        contacts.firstOrNull { it.contactId == contactId }

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

    override suspend fun getAccountSyncInfos(): List<AccountSyncInfo> = emptyList()

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long {
        if (createDelayMillis > 0) kotlinx.coroutines.delay(createDelayMillis)
        created += form
        return created.size.toLong()
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.contact.ContactLink? = null

    override fun observeFavorites(): Flow<Set<Long>> = favorites.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit
}

private fun form(name: String) = ContactForm(name = EditableName(display = name))

@OptIn(ExperimentalCoroutinesApi::class)
class ImportPreviewViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: PreviewContactsRepository,
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

    @Test
    fun `prepare fills entries and keeps invalid blocks`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val codec = FakeVCardCodec(forms = listOf(form("Anna"), form("Bob")))
            val viewModel = viewModel(PreviewContactsRepository(), codec)

            viewModel.prepare("file")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf("file"), codec.parsedTexts)
            assertEquals(listOf("Anna", "Bob"), state.entries.map { it.name })
            assertTrue(state.canImport)
        }

    @Test
    fun `prepare marks duplicates found among contacts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                PreviewContactsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 5,
                            name = "Anna",
                            phones = listOf(PhoneNumber("+79161234567", PhoneKind.MOBILE)),
                        ),
                    ),
                )
            val codec =
                FakeVCardCodec(
                    forms =
                        listOf(
                            ContactForm(
                                name = EditableName(display = "Anna"),
                                phones = listOf(EditablePhone(number = "+79161234567")),
                            ),
                        ),
                )
            val viewModel = viewModel(repository, codec)

            viewModel.prepare("file")
            advanceUntilIdle()

            val entry =
                viewModel.uiState.value.entries
                    .single()
            assertEquals(listOf(5L), entry.duplicates.map { it.contactId })
            assertEquals(1, viewModel.uiState.value.duplicateCount)
        }

    @Test
    fun `decision change switches the entry`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel(PreviewContactsRepository(), FakeVCardCodec(forms = listOf(form("Anna"))))

            viewModel.prepare("file")
            advanceUntilIdle()
            viewModel.onDecisionChange(0, ImportDecision.Skip)
            advanceUntilIdle()

            assertEquals(
                ImportDecision.Skip,
                viewModel.uiState.value.entries
                    .single()
                    .decision,
            )
        }

    @Test
    fun `import creates contacts and reports the outcome`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = PreviewContactsRepository()
            val viewModel = viewModel(repository, FakeVCardCodec(forms = listOf(form("Anna"), form("Bob"))))

            viewModel.prepare("file")
            advanceUntilIdle()
            viewModel.onImport()
            advanceUntilIdle()

            val outcome = viewModel.uiState.value.outcome
            assertEquals(2, outcome?.created)
            assertEquals(listOf("Anna", "Bob"), repository.created.map { it.name.display })
            assertTrue(!viewModel.uiState.value.canImport)
        }

    @Test
    fun `import reports progress until contacts are created`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = PreviewContactsRepository(createDelayMillis = 100)
            val viewModel = viewModel(repository, FakeVCardCodec(forms = listOf(form("Anna"))))

            viewModel.prepare("file")
            advanceUntilIdle()
            viewModel.onImport()
            runCurrent()

            assertTrue(viewModel.uiState.value.isImporting)
            assertFalse(viewModel.uiState.value.canImport)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isImporting)
            assertEquals(
                1,
                viewModel.uiState.value.outcome
                    ?.created,
            )
        }

    @Test
    fun `failed parse reports the error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val codec = FakeVCardCodec(failure = RuntimeException("broken file"))
            val viewModel = viewModel(PreviewContactsRepository(), codec)

            viewModel.prepare("broken")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.failed)
        }
}
