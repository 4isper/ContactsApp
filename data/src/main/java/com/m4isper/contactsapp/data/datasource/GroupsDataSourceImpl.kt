package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.data.datasource.write.planGroupMerge
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Единственное место работы с группами: тонкий адаптер над провайдером.
 * Все вызовы провайдера уходят в IO-диспетчер, план слияния — чистая функция,
 * поэтому путь слияния можно проверить без провайдера контактов.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class GroupsDataSourceImpl(
    private val provider: GroupsProvider,
) : GroupsDataSource {
    override suspend fun loadGroups(): List<ContactGroup> = withContext(Dispatchers.IO) { provider.loadGroups() }

    override suspend fun loadMembership(): Map<Long, Set<Long>> =
        withContext(Dispatchers.IO) { provider.loadMembership() }

    override suspend fun loadContactGroupIds(contactId: Long): Set<Long> =
        withContext(Dispatchers.IO) { provider.loadContactGroupIds(contactId) }

    override suspend fun createGroup(title: String): ContactGroup = createGroup(title, ContactAccount.Local)

    override suspend fun createGroup(
        title: String,
        account: ContactAccount,
    ): ContactGroup =
        withContext(Dispatchers.IO) {
            val id = provider.createGroup(title, account)
            ContactGroup(
                id = id,
                title = title,
                accountName = account.name,
                accountType = account.type,
            )
        }

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ): Unit = withContext(Dispatchers.IO) { provider.renameGroup(groupId, title) }

    override suspend fun deleteGroup(groupId: Long): Unit =
        withContext(Dispatchers.IO) { provider.deleteGroup(groupId) }

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ): Unit = withContext(Dispatchers.IO) { provider.addMembers(groupId, contactIds) }

    override suspend fun removeContactsFromGroup(
        groupId: Long,
        contactIds: List<Long>,
    ): Unit = withContext(Dispatchers.IO) { provider.removeMembers(groupId, contactIds) }

    /**
     * Слияние групп: план строится из данных провайдера и выполняется одной операцией,
     * поэтому перенос членства и удаление исходной группы не расходятся.
     */
    override suspend fun mergeGroups(
        sourceGroupId: Long,
        targetGroupId: Long,
    ): Unit =
        withContext(Dispatchers.IO) {
            if (sourceGroupId == targetGroupId) return@withContext
            val members = provider.membersOf(sourceGroupId)
            val plan =
                planGroupMerge(
                    sourceGroupId = sourceGroupId,
                    targetGroupId = targetGroupId,
                    members = members,
                    rawContacts = provider.rawContactsOf(members),
                    alreadyInTarget = provider.membersAlreadyIn(targetGroupId, members),
                )
            provider.mergeGroups(plan)
        }
}
