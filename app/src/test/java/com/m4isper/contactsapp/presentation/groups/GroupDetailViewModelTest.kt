package com.m4isper.contactsapp.presentation.groups

import com.m4isper.contactsapp.FakeGroupsRepository
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.MergeGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveContactsFromGroupUseCase
import com.m4isper.contactsapp.domain.usecase.RenameGroupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

private class GroupDetailContactsRepository(
    private val contacts: List<ContactDetails>,
) : ContactsRepository,
    FavoritesRepository {
    override suspend fun getContacts(): List<ContactDetails> = contacts

    override suspend fun getContactDetails(contactId: Long): ContactDetails? =
        contacts.firstOrNull { it.contactId == contactId }

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun createContact(form: ContactForm): Long = 1L

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.ContactLink? = null

    override fun observeFavorites(): Flow<Set<Long>> = flowOf(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeGroupsRepository,
        contacts: List<ContactDetails> = emptyList(),
    ) = GroupDetailViewModel(
        getGroupsUseCase = GetGroupsUseCase(repository),
        getMembershipUseCase = GetGroupMembershipUseCase(repository),
        getContactsUseCase = GetContactsUseCase(GroupDetailContactsRepository(contacts)),
        addContactsToGroupUseCase = AddContactsToGroupUseCase(repository),
        removeContactsFromGroupUseCase = RemoveContactsFromGroupUseCase(repository),
        renameGroupUseCase = RenameGroupUseCase(repository),
        deleteGroupUseCase = DeleteGroupUseCase(repository),
        createGroupUseCase = CreateGroupUseCase(repository),
        mergeGroupsUseCase = MergeGroupsUseCase(repository),
    )

    private fun contact(
        id: Long,
        name: String,
    ) = ContactDetails(
        contactId = id,
        name = name,
        phones = listOf(PhoneNumber("+791600000$id", PhoneKind.MOBILE)),
    )

    @Test
    fun `load shows the group members`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            com.m4isper.contactsapp.domain.model
                                .ContactGroup(id = 1, title = "Friends"),
                        ),
                    membership = mapOf(7L to setOf(1L), 8L to setOf(2L)),
                )
            val viewModel = viewModel(groups, listOf(contact(7, "Anna"), contact(8, "Bob")))

            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load(1)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Friends", state.group?.title)
            assertEquals(listOf("Anna"), state.members.map { it.name })
        }

    @Test
    fun `removing a member calls the repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            com.m4isper.contactsapp.domain.model
                                .ContactGroup(id = 1, title = "Friends"),
                        ),
                    membership = mapOf(7L to setOf(1L)),
                )
            val viewModel = viewModel(groups, listOf(contact(7, "Anna")))

            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load(1)
            advanceUntilIdle()
            viewModel.onRemoveMember(7)
            advanceUntilIdle()

            assertEquals(listOf(1L to listOf(7L)), groups.removed)
            assertEquals(GroupDetailEvent.MemberRemoved, viewModel.uiState.value.event)
        }

    @Test
    fun `deleting a group remembers it for the undo`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            com.m4isper.contactsapp.domain.model
                                .ContactGroup(id = 1, title = "Friends"),
                        ),
                    membership = mapOf(7L to setOf(1L)),
                )
            val viewModel = viewModel(groups, listOf(contact(7, "Anna")))

            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load(1)
            advanceUntilIdle()
            viewModel.onDelete()
            advanceUntilIdle()

            val deleted = viewModel.uiState.value.deletedGroup
            assertEquals("Friends", deleted?.title)
            assertEquals(listOf(7L), deleted?.memberIds)
            assertEquals(GroupDetailEvent.GroupDeleted, viewModel.uiState.value.event)
        }

    @Test
    fun `undo restores the group with its members`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            com.m4isper.contactsapp.domain.model
                                .ContactGroup(id = 1, title = "Friends"),
                        ),
                    membership = mapOf(7L to setOf(1L)),
                )
            val viewModel = viewModel(groups, listOf(contact(7, "Anna")))

            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load(1)
            advanceUntilIdle()
            viewModel.onDelete()
            advanceUntilIdle()
            viewModel.onUndoDelete()
            advanceUntilIdle()

            assertTrue(groups.createdTitles.contains("Friends"))
            assertTrue(groups.addedToGroup.any { it.second.contains(7L) })
            assertEquals(GroupDetailEvent.GroupRestored, viewModel.uiState.value.event)
        }

    @Test
    fun `merge moves the group into another one`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val groups =
                FakeGroupsRepository(
                    groups =
                        listOf(
                            com.m4isper.contactsapp.domain.model
                                .ContactGroup(id = 1, title = "Friends"),
                            com.m4isper.contactsapp.domain.model
                                .ContactGroup(id = 2, title = "Work"),
                        ),
                    membership = mapOf(7L to setOf(1L)),
                )
            val viewModel = viewModel(groups, listOf(contact(7, "Anna")))

            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load(1)
            advanceUntilIdle()
            viewModel.onMerge(2)
            advanceUntilIdle()

            assertEquals(listOf(1L to 2L), groups.merged)
            assertEquals(GroupDetailEvent.GroupMerged, viewModel.uiState.value.event)
        }
}
