package com.m4isper.contactsapp.presentation.details

import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactLink
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.contacts.CopyContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.MoveContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ToggleFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetContactGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

private class FakeDetailsRepo2(
    var contacts: List<ContactDetails> = emptyList(),
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

    override suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

    override suspend fun unmergeContacts(link: com.m4isper.contactsapp.domain.model.contact.ContactLink) = Unit

    private val fav = MutableStateFlow<Set<Long>>(emptySet())

    override suspend fun getContacts(): List<ContactDetails> = contacts

    override suspend fun getContactForm(contactId: Long): com.m4isper.contactsapp.domain.model.contact.ContactForm? =
        null

    override suspend fun getContactDetails(contactId: Long): ContactDetails? =
        contacts.firstOrNull {
            it.contactId ==
                contactId
        }

    override fun observeFavorites(): Flow<Set<Long>> = fav.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit

    override suspend fun createContact(
        form: com.m4isper.contactsapp.domain.model.contact.ContactForm,
        account: ContactAccount,
    ): Long = 1L

    override suspend fun updateContact(
        contactId: Long,
        form: com.m4isper.contactsapp.domain.model.contact.ContactForm,
    ) = Unit

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.contact.ContactLink? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailsViewModelExtraTest {
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeDetailsRepo2) =
        ContactDetailsViewModel(
            GetContactDetailsUseCase(repository),
            GetContactGroupsUseCase(FakeGroupsRepository()),
            ToggleFavoriteUseCase(repository),
            ObserveFavoritesUseCase(repository),
            ExportContactsUseCase(
                getContactDetails = GetContactDetailsUseCase(repository),
                loadContactPhoto = LoadContactPhotoUseCase(repository),
                getGroups = GetGroupsUseCase(FakeGroupsRepository()),
                getGroupMembership = GetGroupMembershipUseCase(FakeGroupsRepository()),
                codec = FakeVCardCodec(),
            ),
            GetContactAccountsUseCase(repository),
            CopyContactToAccountUseCase(repository, repository),
            MoveContactToAccountUseCase(CopyContactToAccountUseCase(repository, repository), repository),
        )

    @Test
    fun onEventHandledRuns() =
        runTest(testScheduler) {
            val vm = viewModel(FakeDetailsRepo2())
            vm.onEventHandled()
        }

    @Test
    fun onCopyToAccountWithoutLoadedContactReturns() =
        runTest(testScheduler) {
            val vm = viewModel(FakeDetailsRepo2())
            vm.onCopyToAccount(ContactAccount.Local)
        }

    @Test
    fun onMoveToAccountWithoutLoadedContactReturns() =
        runTest(testScheduler) {
            val vm = viewModel(FakeDetailsRepo2())
            vm.onMoveToAccount(ContactAccount.Local)
        }

    @Test
    fun onCopyToAccountWithLoadedContactRuns() =
        runTest(testScheduler) {
            val repo = FakeDetailsRepo2(contacts = listOf(ContactDetails(contactId = 7, name = "Anna")))
            val vm = viewModel(repo)
            backgroundScope.launch { vm.uiState.collect { } }
            vm.load(7)
            advanceUntilIdle()
            vm.onCopyToAccount(ContactAccount.Local)
            advanceUntilIdle()
        }

    @Test
    fun onMoveToAccountWithLoadedContactRuns() =
        runTest(testScheduler) {
            val repo = FakeDetailsRepo2(contacts = listOf(ContactDetails(contactId = 7, name = "Anna")))
            val vm = viewModel(repo)
            backgroundScope.launch { vm.uiState.collect { } }
            vm.load(7)
            advanceUntilIdle()
            vm.onMoveToAccount(ContactAccount.Local)
            advanceUntilIdle()
        }
}
