package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.data.datasource.write.groups.GroupMergePlan
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверка пути слияния групп: план строится из данных провайдера, а сама операция
 * уходит одним вызовом, поэтому перенос членства и удаление исходной группы
 * не могут разойтись.
 */
class GroupsDataSourceImplTest {
    @Test
    fun `merge moves members to the target group and deletes the source in one operation`() {
        val provider =
            FakeGroupsProvider()
                .withMembers(1, 10, 11)
                .withRawContact(10, 100)
                .withRawContact(11, 101)
                .withMembers(2, 12)
        val dataSource = GroupsDataSourceImpl(provider)

        runBlocking { dataSource.mergeGroups(sourceGroupId = 1, targetGroupId = 2) }

        assertEquals(1, provider.mergePlans.size)
        val plan = provider.mergePlans.single()
        assertEquals(1L, plan.sourceGroupId)
        assertEquals(2L, plan.targetGroupId)
        assertEquals(listOf(100L, 101L), plan.inserts.map { it.rawContactId })
        assertEquals(listOf(2L, 2L), plan.inserts.map { it.groupId })
    }

    @Test
    fun `merge skips members that are already in the target group`() {
        val provider =
            FakeGroupsProvider()
                .withMembers(1, 10, 11)
                .withRawContact(10, 100)
                .withRawContact(11, 101)
                .withMembers(2, 10)
        val dataSource = GroupsDataSourceImpl(provider)

        runBlocking { dataSource.mergeGroups(sourceGroupId = 1, targetGroupId = 2) }

        assertEquals(
            listOf(101L),
            provider
                .mergePlans
                .single()
                .inserts
                .map { it.rawContactId },
        )
    }

    @Test
    fun `merge of a group into itself changes nothing`() {
        val provider = FakeGroupsProvider().withMembers(1, 10)
        val dataSource = GroupsDataSourceImpl(provider)

        runBlocking { dataSource.mergeGroups(sourceGroupId = 1, targetGroupId = 1) }

        assertTrue(provider.mergePlans.isEmpty())
    }

    @Test
    fun `merge of an empty group only deletes it`() {
        val provider = FakeGroupsProvider()
        val dataSource = GroupsDataSourceImpl(provider)

        runBlocking { dataSource.mergeGroups(sourceGroupId = 1, targetGroupId = 2) }

        val plan = provider.mergePlans.single()
        assertTrue(plan.inserts.isEmpty())
        assertEquals(1L, plan.sourceGroupId)
    }

    @Test
    fun `failed merge leaves the provider state unchanged`() {
        val provider =
            FakeGroupsProvider()
                .withMembers(1, 10)
                .withRawContact(10, 100)
                .withMembers(2, 12)
        provider.mergeFailure = IllegalStateException("provider is unavailable")
        val dataSource = GroupsDataSourceImpl(provider)

        val failure =
            runCatching { runBlocking { dataSource.mergeGroups(sourceGroupId = 1, targetGroupId = 2) } }
                .exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        // Операция не разделилась на части: ни переноса, ни отдельного удаления группы.
        assertTrue(provider.mergePlans.isEmpty())
        assertTrue(provider.deletedGroups.isEmpty())
        assertEquals(listOf(10L), provider.membersOf(1))
        assertEquals(listOf(12L), provider.membersOf(2))
    }

    @Test
    fun `members are added and removed through the provider`() {
        val provider = FakeGroupsProvider().withMembers(1, 10)
        val dataSource = GroupsDataSourceImpl(provider)

        runBlocking { dataSource.addContactsToGroup(groupId = 1, contactIds = listOf(11)) }
        runBlocking { dataSource.removeContactsFromGroup(groupId = 1, contactIds = listOf(10)) }

        assertEquals(listOf(1L to listOf(11L)), provider.addedMembers)
        assertEquals(listOf(1L to listOf(10L)), provider.removedMembers)
    }

    @Test
    fun `created group keeps the account and the new id`() {
        val provider = FakeGroupsProvider()
        val dataSource = GroupsDataSourceImpl(provider)
        val account = ContactAccount(name = "user@gmail.com", type = "com.google")

        val group = runBlocking { dataSource.createGroup(title = "Work", account = account) }

        assertEquals(7L, group.id)
        assertEquals("Work", group.title)
        assertEquals("user@gmail.com", group.accountName)
        assertEquals("com.google", group.accountType)
        assertEquals(listOf("Work" to account), provider.createdGroups)
    }

    @Test
    fun `membership is read from the provider`() {
        val provider = FakeGroupsProvider().withMembers(1, 10, 11).withMembers(2, 10, 12)
        val dataSource = GroupsDataSourceImpl(provider)

        val membership = runBlocking { dataSource.loadMembership() }

        assertEquals(setOf(1L, 2L), membership[10L])
        assertEquals(setOf(1L), membership[11L])
        assertEquals(setOf(2L), membership[12L])
        assertFalse(membership.containsKey(99L))
    }

    /** Провайдер-заглушка: хранит состояние групп и применяет план слияния целиком. */
    private class FakeGroupsProvider : GroupsProvider {
        private val members = mutableMapOf<Long, MutableSet<Long>>()
        private val rawContacts = mutableMapOf<Long, Long>()
        val createdGroups = mutableListOf<Pair<String, ContactAccount>>()
        val mergePlans = mutableListOf<GroupMergePlan>()
        val deletedGroups = mutableListOf<Long>()
        val addedMembers = mutableListOf<Pair<Long, List<Long>>>()
        val removedMembers = mutableListOf<Pair<Long, List<Long>>>()
        var mergeFailure: RuntimeException? = null

        fun withMembers(
            groupId: Long,
            vararg contactIds: Long,
        ): FakeGroupsProvider {
            members.getOrPut(groupId) { mutableSetOf() } += contactIds.toSet()
            return this
        }

        fun withRawContact(
            contactId: Long,
            rawContactId: Long,
        ): FakeGroupsProvider {
            rawContacts[contactId] = rawContactId
            return this
        }

        override fun loadGroups(): List<ContactGroup> = emptyList()

        override fun loadMembership(): Map<Long, Set<Long>> =
            members
                .flatMap { (groupId, contacts) -> contacts.map { it to groupId } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, groups) -> groups.toSet() }

        override fun loadContactGroupIds(contactId: Long): Set<Long> =
            members.filterValues { contactId in it }.keys.toSet()

        override fun createGroup(
            title: String,
            account: ContactAccount,
        ): Long {
            createdGroups += title to account
            return 7L
        }

        override fun renameGroup(
            groupId: Long,
            title: String,
        ) = Unit

        override fun deleteGroup(groupId: Long) {
            deletedGroups += groupId
        }

        override fun addMembers(
            groupId: Long,
            contactIds: List<Long>,
        ) {
            addedMembers += groupId to contactIds
        }

        override fun removeMembers(
            groupId: Long,
            contactIds: List<Long>,
        ) {
            removedMembers += groupId to contactIds
        }

        override fun membersOf(groupId: Long): List<Long> = members[groupId].orEmpty().toList()

        override fun rawContactsOf(contactIds: List<Long>): Map<Long, Long> =
            contactIds.mapNotNull { id -> rawContacts[id]?.let { id to it } }.toMap()

        override fun membersAlreadyIn(
            groupId: Long,
            contactIds: List<Long>,
        ): Set<Long> = members[groupId].orEmpty()

        override fun mergeGroups(plan: GroupMergePlan) {
            mergeFailure?.let { throw it }
            mergePlans += plan
            // Провайдер применяет операцию целиком: членства переносятся, группа удаляется.
            plan.inserts.forEach { insert ->
                val contactId = rawContacts.entries.firstOrNull { it.value == insert.rawContactId }?.key
                if (contactId != null) {
                    members.getOrPut(insert.groupId) { mutableSetOf() } += contactId
                }
            }
            members.remove(plan.sourceGroupId)
        }
    }
}
