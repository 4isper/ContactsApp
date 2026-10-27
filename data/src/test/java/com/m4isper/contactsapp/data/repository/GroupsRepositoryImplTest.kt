package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.GroupsDataSource
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupsRepositoryImplTest {
    private class FakeDs : GroupsDataSource {
        val groups = mutableListOf(ContactGroup(id = 1L, title = "A"), ContactGroup(id = 2L, title = "B"))
        val created = mutableListOf<Pair<String, ContactAccount>>()
        val renamed = mutableListOf<Pair<Long, String>>()
        val deleted = mutableListOf<Long>()
        val added = mutableListOf<Pair<Long, List<Long>>>()
        val removed = mutableListOf<Pair<Long, List<Long>>>()
        var merged: Pair<Long, Long>? = null

        override suspend fun loadGroups(): List<ContactGroup> = groups.toList()

        override suspend fun loadMembership(): Map<Long, Set<Long>> = emptyMap()

        override suspend fun loadContactGroupIds(contactId: Long): Set<Long> =
            if (contactId ==
                10L
            ) {
                setOf(2L)
            } else {
                emptySet()
            }

        override suspend fun createGroup(title: String): ContactGroup = createGroup(title, ContactAccount.Local)

        override suspend fun createGroup(
            title: String,
            account: ContactAccount,
        ): ContactGroup {
            created += title to account
            return ContactGroup(id = 9L, title = title, accountName = account.name, accountType = account.type)
        }

        override suspend fun renameGroup(
            groupId: Long,
            title: String,
        ) {
            renamed += groupId to title
        }

        override suspend fun deleteGroup(groupId: Long) {
            deleted += groupId
        }

        override suspend fun addContactsToGroup(
            groupId: Long,
            contactIds: List<Long>,
        ) {
            added += groupId to contactIds
        }

        override suspend fun removeContactsFromGroup(
            groupId: Long,
            contactIds: List<Long>,
        ) {
            removed +=
                groupId to contactIds
        }

        override suspend fun mergeGroups(
            sourceGroupId: Long,
            targetGroupId: Long,
        ) {
            merged =
                sourceGroupId to targetGroupId
        }
    }

    @Test
    fun getContactGroupsFiltersByMembership() =
        runBlocking {
            val ds = FakeDs()
            val repo = GroupsRepositoryImpl(ds)
            assertEquals(listOf(2L), repo.getContactGroups(10L).map { it.id })
        }

    @Test
    fun getContactGroupsEmptyWhenNoMembership() =
        runBlocking {
            val ds = FakeDs()
            val repo = GroupsRepositoryImpl(ds)
            assertTrue(repo.getContactGroups(99L).isEmpty())
        }

    @Test
    fun createGroupTrimsTitle() =
        runBlocking {
            val ds = FakeDs()
            val repo = GroupsRepositoryImpl(ds)
            repo.createGroup("  x  ")
            assertEquals("x", ds.created.first().first)
        }

    @Test
    fun renameGroupTrimsTitle() =
        runBlocking {
            val ds = FakeDs()
            val repo = GroupsRepositoryImpl(ds)
            repo.renameGroup(1L, "  y  ")
            assertEquals("y", ds.renamed.first().second)
        }

    @Test
    fun deleteGroupDelegates() =
        runBlocking {
            val ds = FakeDs()
            val repo = GroupsRepositoryImpl(ds)
            repo.deleteGroup(3L)
            assertEquals(listOf(3L), ds.deleted)
        }

    @Test
    fun addAndRemoveDelegates() =
        runBlocking {
            val ds = FakeDs()
            val repo = GroupsRepositoryImpl(ds)
            repo.addContactsToGroup(1L, listOf(2L))
            repo.removeContactsFromGroup(1L, listOf(3L))
            assertEquals(listOf(2L), ds.added.first().second)
            assertEquals(listOf(3L), ds.removed.first().second)
        }

    @Test
    fun mergeGroupsDelegates() =
        runBlocking {
            val ds = FakeDs()
            val repo = GroupsRepositoryImpl(ds)
            repo.mergeGroups(1L, 2L)
            assertEquals(1L to 2L, ds.merged)
        }
}
