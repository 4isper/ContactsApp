package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.GroupsDataSource
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class GroupsRepositoryImpl(
    private val dataSource: GroupsDataSource,
) : GroupsRepository {
    override suspend fun getGroups(): List<ContactGroup> = dataSource.loadGroups()

    override suspend fun getMembership(): Map<Long, Set<Long>> = dataSource.loadMembership()

    override suspend fun getContactGroups(contactId: Long): List<ContactGroup> {
        val groupIds = dataSource.loadContactGroupIds(contactId)
        if (groupIds.isEmpty()) return emptyList()
        return dataSource.loadGroups().filter { it.id in groupIds }
    }

    override suspend fun createGroup(title: String): ContactGroup = createGroup(title, ContactAccount.Local)

    override suspend fun createGroup(
        title: String,
        account: ContactAccount,
    ): ContactGroup = dataSource.createGroup(title.trim(), account)

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ) = dataSource.renameGroup(groupId, title.trim())

    override suspend fun deleteGroup(groupId: Long) = dataSource.deleteGroup(groupId)

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) = dataSource.addContactsToGroup(groupId, contactIds)

    override suspend fun removeContactsFromGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) = dataSource.removeContactsFromGroup(groupId, contactIds)

    override suspend fun mergeGroups(
        sourceGroupId: Long,
        targetGroupId: Long,
    ) = dataSource.mergeGroups(sourceGroupId, targetGroupId)
}
