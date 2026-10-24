package com.m4isper.contactsapp.presentation.details

import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.FakeVCardCodec
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.ContactLink
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
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
import com.m4isper.contactsapp.presentation.common.Loadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeDetailsRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var failure: Throwable? = null,
    initialFavorites: Set<Long> = emptySet(),
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

    override suspend fun getAccountSyncInfos(): List<AccountSyncInfo> = emptyList()

    override suspend fun unmergeContacts(link: ContactLink) = Unit

    private val favoritesFlow = MutableStateFlow(initialFavorites)

    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
        failure?.let { throw it }
        return contacts.firstOrNull { it.contactId == contactId }
    }

    override fun observeFavorites(): Flow<Set<Long>> = favoritesFlow.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) {
        val current = favoritesFlow.value
        favoritesFlow.value =
            if (contactId in current) {
                current - contactId
            } else {
                current + contactId
            }
    }

    override suspend fun removeFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value - contactId
    }

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long = 1L

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
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
class ContactDetailsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeDetailsRepository,
        groups: FakeGroupsRepository = FakeGroupsRepository(),
        codec: FakeVCardCodec = FakeVCardCodec(),
    ) = ContactDetailsViewModel(
        GetContactDetailsUseCase(repository),
        GetContactGroupsUseCase(groups),
        ToggleFavoriteUseCase(repository),
        ObserveFavoritesUseCase(repository),
        ExportContactsUseCase(
            getContactDetails = GetContactDetailsUseCase(repository),
            loadContactPhoto = LoadContactPhotoUseCase(repository),
            getGroups = GetGroupsUseCase(groups),
            getGroupMembership = GetGroupMembershipUseCase(groups),
            codec = codec,
        ),
        GetContactAccountsUseCase(repository),
        CopyContactToAccountUseCase(repository, repository),
        MoveContactToAccountUseCase(CopyContactToAccountUseCase(repository, repository), repository),
    )

    @Test
    fun `load emits contact content`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDetailsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 7,
                            name = "Anna",
                            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
                        ),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Content)
            assertEquals("Anna", (content as Loadable.Content).data.name)
        }

    @Test
    fun `load unknown id emits not found`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel(FakeDetailsRepository())
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(42)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Empty)
        }

    @Test
    fun `load failure emits error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeDetailsRepository(failure = SecurityException("denied"))
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()

            val content = viewModel.uiState.value.content
            assertTrue(content is Loadable.Error)
            assertEquals(AppError.PERMISSION_DENIED, (content as Loadable.Error).error)
        }

    @Test
    fun `load different id refetches`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            var calls = 0
            val repository =
                object : ContactsReader, ContactsAccounts, ContactsWriter, ContactsMerger, FavoritesRepository {
                    override suspend fun getContactsPage(
                        offset: Int,
                        limit: Int,
                    ): List<ContactDetails> = emptyList()

                    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

                    override suspend fun getProfileContactId(): Long? = null

                    override suspend fun getContactAccount(contactId: Long): ContactAccount = ContactAccount.Local

                    override suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

                    override suspend fun getAccountSyncInfos(): List<AccountSyncInfo> = emptyList()

                    override suspend fun unmergeContacts(link: ContactLink) = Unit

                    override suspend fun createContact(
                        form: ContactForm,
                        account: ContactAccount,
                    ): Long = 1L

                    override suspend fun getContacts(): List<ContactDetails> = emptyList()

                    override suspend fun getContactForm(contactId: Long): ContactForm? = null

                    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
                        calls++
                        return null
                    }

                    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet())

                    override suspend fun toggleFavorite(contactId: Long) = Unit

                    override suspend fun removeFavorite(contactId: Long) = Unit

                    override suspend fun setContactPhoto(
                        contactId: Long,
                        photo: ByteArray,
                    ) = Unit

                    override suspend fun updateContact(
                        contactId: Long,
                        form: ContactForm,
                    ) = Unit

                    override suspend fun deleteContact(contactId: Long) = Unit

                    override suspend fun mergeContacts(
                        targetId: Long,
                        sourceIds: List<Long>,
                    ): com.m4isper.contactsapp.domain.model.contact.ContactLink? = null
                }
            val viewModel =
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
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()
            viewModel.load(8)
            advanceUntilIdle()

            assertEquals(2, calls)
        }

    @Test
    fun `toggle favorite flips star state`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDetailsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 7,
                            name = "Anna",
                            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
                        ),
                    ),
                )
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load(7)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.isFavorite)

            viewModel.toggleFavorite()
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.isFavorite)
        }

    @Test
    fun `vCardText returns encoded vcard of loaded contact and null otherwise`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDetailsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 7,
                            name = "Anna",
                            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
                        ),
                    ),
                )
            val codec = FakeVCardCodec(encoded = "CARD")
            val viewModel = viewModel(repository, codec = codec)
            backgroundScope.launch { viewModel.uiState.collect { } }

            // Пока контакт не загружен, в порт не обращаемся.
            assertEquals(null, viewModel.vCardText())
            assertEquals(emptyList<List<ContactDetails>>(), codec.encodedContacts)

            viewModel.load(7)
            advanceUntilIdle()

            assertEquals("CARD", viewModel.vCardText())
            assertEquals(listOf(7L), codec.encodedContacts.single().map { it.details.contactId })
        }

    @Test
    fun `load emits contact groups`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDetailsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 7,
                            name = "Anna",
                            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
                        ),
                    ),
                )
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            ContactGroup(id = 100, title = "Семья"),
                            ContactGroup(id = 200, title = "Работа"),
                        ),
                    membership = mapOf(7L to setOf(100L)),
                )
            val viewModel = viewModel(repository, groups)
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()

            assertEquals(listOf(ContactGroup(id = 100, title = "Семья")), viewModel.uiState.value.groups)
        }

    @Test
    fun `group read failure keeps contact visible`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeDetailsRepository(
                    listOf(
                        ContactDetails(
                            contactId = 7,
                            name = "Anna",
                            phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
                        ),
                    ),
                )
            val viewModel = viewModel(repository, groups = FakeGroupsRepository(failure = RuntimeException("boom")))
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.load(7)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.content is Loadable.Content)
            assertEquals(emptyList<ContactGroup>(), viewModel.uiState.value.groups)
        }
}
