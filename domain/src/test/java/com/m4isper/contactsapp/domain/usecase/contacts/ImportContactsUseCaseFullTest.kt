package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.ImportDecision
import com.m4isper.contactsapp.domain.model.contact.ImportEntry
import com.m4isper.contactsapp.domain.model.contact.ImportFailure
import com.m4isper.contactsapp.domain.model.contact.ImportFailureReason
import com.m4isper.contactsapp.domain.model.contact.ImportOutcome
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.vcard.VCardContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class FakeRepo :
    ContactsReader,
    ContactsAccounts,
    ContactsWriter,
    ContactsMerger,
    FavoritesRepository {
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

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun getContactDetails(
        contactId: Long,
    ): com.m4isper.contactsapp.domain.model.contact.ContactDetails? = null

    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long = 42L

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

private fun groupsRepo(groups: List<ContactGroup> = emptyList()): GroupsRepository =
    object : GroupsRepository {
        override suspend fun getGroups(): List<ContactGroup> = groups

        override suspend fun getMembership(): Map<Long, Set<Long>> = emptyMap()

        override suspend fun getContactGroups(contactId: Long): List<ContactGroup> = emptyList()

        override suspend fun createGroup(title: String): ContactGroup = ContactGroup(id = 1L, title = title)

        override suspend fun createGroup(
            title: String,
            account: ContactAccount,
        ): ContactGroup = createGroup(title)

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
    }

class ImportContactsUseCaseFullTest {
    @Test
    fun emptyEntries() =
        runBlocking {
            val repo = FakeRepo()
            val uc =
                ImportContactsUseCase(
                    createContact = CreateContactUseCase(repo),
                    mergeContacts = MergeContactsUseCase(repo),
                    setContactPhoto = SetContactPhotoUseCase(repo),
                    addContactsToGroup = AddContactsToGroupUseCase(groupsRepo()),
                    getGroups = GetGroupsUseCase(groupsRepo()),
                    deleteContacts = DeleteContactsUseCase(repo, repo),
                )
            val outcome = uc(emptyList())
            assertEquals(0, outcome.created)
            assertEquals(0, outcome.merged)
            assertEquals(0, outcome.skipped)
            assertTrue(outcome.failures.isEmpty())
        }

    @Test
    fun skipDecision() =
        runBlocking {
            val repo = FakeRepo()
            val uc =
                ImportContactsUseCase(
                    createContact = CreateContactUseCase(repo),
                    mergeContacts = MergeContactsUseCase(repo),
                    setContactPhoto = SetContactPhotoUseCase(repo),
                    addContactsToGroup = AddContactsToGroupUseCase(groupsRepo()),
                    getGroups = GetGroupsUseCase(groupsRepo()),
                    deleteContacts = DeleteContactsUseCase(repo, repo),
                )
            val entry = ImportEntry(contact = VCardContact(form = ContactForm()), decision = ImportDecision.Skip)
            val outcome = uc(listOf(entry))
            assertEquals(1, outcome.skipped)
        }

    @Test
    fun importOutcomeDataClass() {
        val outcome =
            ImportOutcome(created = 1, merged = 2, skipped = 3, failures = emptyList(), skippedCategories = emptyList())
        assertEquals(1, outcome.created)
        assertEquals(2, outcome.merged)
        assertEquals(3, outcome.skipped)
        assertEquals(0, ImportOutcome().created)
        assertTrue(outcome.toString().isNotEmpty())
    }

    @Test
    fun importFailureReasons() {
        assertEquals(5, ImportFailureReason.values().size)
        assertEquals(ImportFailureReason.INVALID, ImportFailureReason.valueOf("INVALID"))
        assertEquals(ImportFailureReason.FAILED, ImportFailureReason.valueOf("FAILED"))
        assertEquals(ImportFailureReason.MERGE_FAILED, ImportFailureReason.valueOf("MERGE_FAILED"))
        assertEquals(ImportFailureReason.PHOTO_FAILED, ImportFailureReason.valueOf("PHOTO_FAILED"))
        assertEquals(ImportFailureReason.GROUP_FAILED, ImportFailureReason.valueOf("GROUP_FAILED"))
    }

    @Test
    fun importFailure() {
        val f = ImportFailure(name = "n", reason = ImportFailureReason.INVALID)
        assertEquals(f, ImportFailure(name = "n", reason = ImportFailureReason.INVALID))
        assertEquals(f.hashCode(), ImportFailure(name = "n", reason = ImportFailureReason.INVALID).hashCode())
        assertTrue(f.toString().isNotEmpty())
    }

    @Test
    fun importDecisionTypes() {
        assertEquals(ImportDecision.Create, ImportDecision.Create)
        assertEquals(ImportDecision.Skip, ImportDecision.Skip)
        assertEquals(ImportDecision.Merge(targetId = 5L), ImportDecision.Merge(targetId = 5L))
    }
}
