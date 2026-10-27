package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.data.datasource.write.groups.GroupMergePlan
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupsDataSourceImplExtraTest {
    private class FakeProvider : GroupsProvider {
        override fun loadGroups(): List<ContactGroup> = emptyList()

        override fun loadMembership(): Map<Long, Set<Long>> = emptyMap()

        override fun loadContactGroupIds(contactId: Long): Set<Long> = emptySet()

        override fun createGroup(
            title: String,
            account: ContactAccount,
        ): Long = 7L

        override fun renameGroup(
            groupId: Long,
            title: String,
        ) = Unit

        override fun deleteGroup(groupId: Long) = Unit

        override fun addMembers(
            groupId: Long,
            contactIds: List<Long>,
        ) = Unit

        override fun removeMembers(
            groupId: Long,
            contactIds: List<Long>,
        ) = Unit

        override fun membersOf(groupId: Long): List<Long> = emptyList()

        override fun rawContactsOf(contactIds: List<Long>): Map<Long, Long> = emptyMap()

        override fun membersAlreadyIn(
            groupId: Long,
            contactIds: List<Long>,
        ): Set<Long> = emptySet()

        override fun mergeGroups(plan: GroupMergePlan) = Unit
    }

    @Test
    fun allMethodsDelegateToProvider() =
        runBlocking {
            val ds = GroupsDataSourceImpl(FakeProvider())
            assertTrue(ds.loadGroups().isEmpty())
            assertTrue(ds.loadMembership().isEmpty())
            assertTrue(ds.loadContactGroupIds(1L).isEmpty())
            assertEquals("t", ds.createGroup("t").title)
            assertEquals(7L, ds.createGroup("t", ContactAccount.Local).id)
            ds.renameGroup(1L, "x")
            ds.deleteGroup(1L)
            ds.addContactsToGroup(1L, listOf(2L))
            ds.removeContactsFromGroup(1L, listOf(2L))
            ds.mergeGroups(1L, 2L)
        }
}
