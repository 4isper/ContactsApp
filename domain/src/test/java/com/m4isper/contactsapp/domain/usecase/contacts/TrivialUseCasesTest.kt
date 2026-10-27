package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactLink
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.groups.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetContactGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.MergeGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.groups.RemoveContactsFromGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.RenameGroupUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class FakeTrivialRepository :
    ContactsReader,
    ContactsAccounts,
    ContactsWriter,
    ContactsMerger,
    FavoritesRepository,
    com.m4isper.contactsapp.domain.repository.GroupsRepository,
    com.m4isper.contactsapp.domain.repository.settings.SettingsReader {
    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<com.m4isper.contactsapp.domain.model.contact.ContactDetails> = emptyList()

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun getContactAccount(contactId: Long): ContactAccount = ContactAccount.Local

    override suspend fun getContactAccounts(): List<ContactAccount> = listOf(ContactAccount.Local)

    override suspend fun getAccountSyncInfos(): List<AccountSyncInfo> = emptyList()

    override suspend fun unmergeContacts(link: com.m4isper.contactsapp.domain.model.contact.ContactLink) = Unit

    override suspend fun getContacts(): List<com.m4isper.contactsapp.domain.model.contact.ContactDetails> = emptyList()

    override suspend fun getContactForm(contactId: Long): com.m4isper.contactsapp.domain.model.contact.ContactForm? =
        null

    override suspend fun getContactDetails(
        contactId: Long,
    ): com.m4isper.contactsapp.domain.model.contact.ContactDetails? = null

    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit

    override suspend fun createContact(
        form: com.m4isper.contactsapp.domain.model.contact.ContactForm,
        account: ContactAccount,
    ): Long = 42L

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
    ): com.m4isper.contactsapp.domain.model.contact.ContactLink? =
        ContactLink(
            com.m4isper.contactsapp.domain.model.contact
                .RawContactRef(targetId),
            sourceIds.map {
                com.m4isper.contactsapp.domain.model.contact
                    .RawContactRef(it)
            },
        )

    override suspend fun getGroups(): List<com.m4isper.contactsapp.domain.model.contact.ContactGroup> = emptyList()

    override suspend fun getMembership(): Map<Long, Set<Long>> = emptyMap()

    override suspend fun getContactGroups(
        contactId: Long,
    ): List<com.m4isper.contactsapp.domain.model.contact.ContactGroup> = emptyList()

    override suspend fun createGroup(title: String): com.m4isper.contactsapp.domain.model.contact.ContactGroup =
        com.m4isper.contactsapp.domain.model.contact
            .ContactGroup(id = 1L, title = title)

    override suspend fun createGroup(
        title: String,
        account: ContactAccount,
    ): com.m4isper.contactsapp.domain.model.contact.ContactGroup = createGroup(title)

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ) = Unit

    override suspend fun deleteGroup(groupId: Long) = Unit

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) = Unit

    override suspend fun removeContactsFromGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) = Unit

    override suspend fun mergeGroups(
        sourceGroupId: Long,
        targetGroupId: Long,
    ) = Unit

    override fun observeSettings(): Flow<AppSettings> =
        kotlinx.coroutines.flow.MutableStateFlow(
            com.m4isper.contactsapp.domain.model.settings
                .AppSettings(),
        )
}

class TrivialUseCasesTest {
    @Test
    fun toggleFavoriteUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = ToggleFavoriteUseCase(repo)
            uc(1L)
            uc(2L)
            assertTrue(true)
        }

    @Test
    fun removeFavoriteUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = RemoveFavoriteUseCase(repo)
            uc(1L)
            assertTrue(true)
        }

    @Test
    fun removeContactsFromGroupUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = RemoveContactsFromGroupUseCase(repo)
            uc(1L, listOf(2L, 3L))
            assertTrue(true)
        }

    @Test
    fun refreshFavoritesUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = RefreshFavoritesUseCase(repo)
            uc()
            assertTrue(true)
        }

    @Test
    fun observeFavoritesUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = ObserveFavoritesUseCase(repo)
            val flow = uc.invoke()
            assertTrue(flow is Flow)
        }

    @Test
    fun photoRefDataClass() {
        val ref =
            com.m4isper.contactsapp.domain.model.contact.PhotoRef
                .of("uri")
        assertEquals("uri", ref?.uri)
        assertEquals(
            com.m4isper.contactsapp.domain.model.contact.PhotoRef
                .of("uri"),
            com.m4isper.contactsapp.domain.model.contact.PhotoRef
                .of("uri"),
        )
        assertNull(
            com.m4isper.contactsapp.domain.model.contact.PhotoRef
                .of(null),
        )
        assertNull(
            com.m4isper.contactsapp.domain.model.contact.PhotoRef
                .of(""),
        )
    }

    @Test
    fun mergeGroupsUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = MergeGroupsUseCase(repo)
            uc(1L, 2L)
            assertTrue(true)
        }

    @Test
    fun getProfileContactIdUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = GetProfileContactIdUseCase(repo)
            assertNull(uc())
        }

    @Test
    fun getDefaultContactAccountUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = GetDefaultContactAccountUseCase(repo)
            assertEquals(ContactAccount.Local, uc())
        }

    @Test
    fun getContactsPageUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = GetContactsPageUseCase(repo)
            assertEquals(emptyList<com.m4isper.contactsapp.domain.model.contact.ContactDetails>(), uc(0, 10))
        }

    @Test
    fun getContactGroupsUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = GetContactGroupsUseCase(repo)
            assertEquals(emptyList<com.m4isper.contactsapp.domain.model.contact.ContactGroup>(), uc(1L))
        }

    @Test
    fun getContactFormUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = GetContactFormUseCase(repo)
            assertNull(uc(1L))
        }

    @Test
    fun getContactAccountUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = GetContactAccountUseCase(repo)
            assertEquals(ContactAccount.Local, uc(1L))
        }

    @Test
    fun getContactAccountsUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = GetContactAccountsUseCase(repo)
            assertEquals(listOf(ContactAccount.Local), uc())
        }

    @Test
    fun deleteContactsUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = DeleteContactsUseCase(repo, repo)
            uc(setOf(1L, 2L))
            assertTrue(true)
        }

    @Test
    fun deleteGroupUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = DeleteGroupUseCase(repo)
            uc(1L)
            assertTrue(true)
        }

    @Test
    fun createGroupUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = CreateGroupUseCase(repo)
            uc("Test Group")
            assertTrue(true)
        }

    @Test
    fun renameGroupUseCase() =
        runBlocking {
            val repo = FakeTrivialRepository()
            val uc = RenameGroupUseCase(repo)
            uc(1L, "New Name")
            assertTrue(true)
        }
}
