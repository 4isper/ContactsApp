package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.GroupsDataSource
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupsRepositoryImplTest {
    private class RecordingGroupsDataSource(
        private val failure: Throwable? = null,
    ) : GroupsDataSource {
        var createdTitle: String? = null
        var createdAccount: ContactAccount? = null

        override suspend fun loadGroups(): List<ContactGroup> {
            failure?.let { throw it }
            return emptyList()
        }

        override suspend fun loadMembership(): Map<Long, Set<Long>> = emptyMap()

        override suspend fun loadContactGroupIds(contactId: Long): Set<Long> = emptySet()

        override suspend fun createGroup(title: String): ContactGroup = createGroup(title, ContactAccount.Local)

        override suspend fun createGroup(
            title: String,
            account: ContactAccount,
        ): ContactGroup {
            failure?.let { throw it }
            createdTitle = title
            createdAccount = account
            return ContactGroup(
                id = 1,
                title = title,
                accountName = account.name,
                accountType = account.type,
            )
        }

        override suspend fun renameGroup(
            groupId: Long,
            title: String,
        ) = Unit

        override suspend fun deleteGroup(groupId: Long) = Unit

        override suspend fun removeContactsFromGroup(
            groupId: Long,
            contactIds: List<Long>,
        ) = Unit

        override suspend fun mergeGroups(
            sourceGroupId: Long,
            targetGroupId: Long,
        ) {
            failure?.let { throw it }
        }

        override suspend fun addContactsToGroup(
            groupId: Long,
            contactIds: List<Long>,
        ) = Unit
    }

    @Test
    fun `createGroup forwards trimmed title and selected account`() =
        runBlocking {
            val dataSource = RecordingGroupsDataSource()
            val repository = GroupsRepositoryImpl(dataSource)
            val google = ContactAccount(name = "user@gmail.com", type = "com.google")

            val group = repository.createGroup("  Работа  ", google)

            assertEquals("Работа", dataSource.createdTitle)
            assertEquals(google, dataSource.createdAccount)
            assertEquals(
                ContactGroup(
                    id = 1,
                    title = "Работа",
                    accountName = google.name,
                    accountType = google.type,
                ),
                group,
            )
        }

    @Test
    fun `a failed read of the groups is propagated`() {
        val repository = GroupsRepositoryImpl(RecordingGroupsDataSource(failure = SecurityException("denied")))

        val failure = runCatching { runBlocking { repository.getGroups() } }.exceptionOrNull()

        assertTrue(failure is SecurityException)
    }

    @Test
    fun `a failed creation of a group is propagated`() {
        val repository = GroupsRepositoryImpl(RecordingGroupsDataSource(failure = RuntimeException("boom")))

        val failure = runCatching { runBlocking { repository.createGroup("Work") } }.exceptionOrNull()

        assertTrue(failure is RuntimeException)
    }

    @Test
    fun `a failed merge of the groups is propagated`() {
        val repository = GroupsRepositoryImpl(RecordingGroupsDataSource(failure = RuntimeException("boom")))

        val failure = runCatching { runBlocking { repository.mergeGroups(1, 2) } }.exceptionOrNull()

        assertTrue(failure is RuntimeException)
    }
}
