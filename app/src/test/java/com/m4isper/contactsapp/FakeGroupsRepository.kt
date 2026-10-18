package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.repository.GroupsRepository

/** Фейк репозитория групп: in-memory состояние и запись вызовов для проверок. */
class FakeGroupsRepository(
    var groups: List<ContactGroup> = emptyList(),
    var membership: Map<Long, Set<Long>> = emptyMap(),
    var failure: Throwable? = null,
) : GroupsRepository {
    val addedToGroup = mutableListOf<Pair<Long, List<Long>>>()
    val renamed = mutableListOf<Pair<Long, String>>()
    val deleted = mutableListOf<Long>()

    override suspend fun getGroups(): List<ContactGroup> {
        failure?.let { throw it }
        return groups
    }

    override suspend fun getMembership(): Map<Long, Set<Long>> {
        failure?.let { throw it }
        return membership
    }

    override suspend fun getContactGroups(contactId: Long): List<ContactGroup> {
        failure?.let { throw it }
        return groups.filter { it.id in membership[contactId].orEmpty() }
    }

    override suspend fun createGroup(title: String): ContactGroup {
        failure?.let { throw it }
        val group = ContactGroup(id = (groups.maxOfOrNull { it.id } ?: 0L) + 1, title = title)
        groups = groups + group
        return group
    }

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ) {
        failure?.let { throw it }
        renamed += groupId to title
        groups = groups.map { if (it.id == groupId) it.copy(title = title) else it }
    }

    override suspend fun deleteGroup(groupId: Long) {
        failure?.let { throw it }
        deleted += groupId
        groups = groups.filterNot { it.id == groupId }
    }

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) {
        failure?.let { throw it }
        addedToGroup += groupId to contactIds
        val updated = membership.toMutableMap()
        contactIds.forEach { contactId ->
            updated[contactId] = updated[contactId].orEmpty() + groupId
        }
        membership = updated
    }
}
