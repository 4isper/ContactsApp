package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.ImportDecision
import com.m4isper.contactsapp.domain.model.contact.ImportEntry
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.vcard.VCardContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class FakeImportRepo :
    ContactsWriter,
    ContactsMerger,
    FavoritesRepository,
    GroupsRepository {
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
    ) = null

    override suspend fun unmergeContacts(link: com.m4isper.contactsapp.domain.model.contact.ContactLink) = Unit

    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit

    override suspend fun getGroups(): List<ContactGroup> = emptyList()

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

class ImportContactsUseCaseTest {
    @Test
    fun emptyEntriesReturnsZeroOutcome() =
        runTest {
            val repo = FakeImportRepo()
            val useCase =
                ImportContactsUseCase(
                    createContact = CreateContactUseCase(repo),
                    mergeContacts = MergeContactsUseCase(repo),
                    setContactPhoto = SetContactPhotoUseCase(repo),
                    addContactsToGroup = AddContactsToGroupUseCase(repo),
                    getGroups = GetGroupsUseCase(repo),
                    deleteContacts = DeleteContactsUseCase(repo, repo),
                )
            val outcome = useCase(emptyList())
            assertEquals(0, outcome.created)
            assertEquals(0, outcome.merged)
            assertEquals(0, outcome.skipped)
        }

    @Test
    fun skipDecisionIncrementsSkipped() =
        runTest {
            val repo = FakeImportRepo()
            val useCase =
                ImportContactsUseCase(
                    createContact = CreateContactUseCase(repo),
                    mergeContacts = MergeContactsUseCase(repo),
                    setContactPhoto = SetContactPhotoUseCase(repo),
                    addContactsToGroup = AddContactsToGroupUseCase(repo),
                    getGroups = GetGroupsUseCase(repo),
                    deleteContacts = DeleteContactsUseCase(repo, repo),
                )
            val entry = ImportEntry(contact = VCardContact(form = ContactForm()), decision = ImportDecision.Skip)
            val outcome = useCase(listOf(entry))
            assertEquals(1, outcome.skipped)
        }

    @Test
    fun createDecisionCreatesContact() =
        runTest {
            val repo = FakeImportRepo()
            val useCase =
                ImportContactsUseCase(
                    createContact = CreateContactUseCase(repo),
                    mergeContacts = MergeContactsUseCase(repo),
                    setContactPhoto = SetContactPhotoUseCase(repo),
                    addContactsToGroup = AddContactsToGroupUseCase(repo),
                    getGroups = GetGroupsUseCase(repo),
                    deleteContacts = DeleteContactsUseCase(repo, repo),
                )
            val form =
                ContactForm(
                    name =
                        com.m4isper.contactsapp.domain.model.contact
                            .EditableName(display = "John"),
                )
            val entry = ImportEntry(contact = VCardContact(form = form), decision = ImportDecision.Create)
            val outcome = useCase(listOf(entry))
            assertEquals(1, outcome.created)
        }
}
