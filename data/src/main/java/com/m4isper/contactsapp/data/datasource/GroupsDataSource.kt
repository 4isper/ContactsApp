package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup

/** Группы (метки) контактов и членство в них. */
interface GroupsDataSource {
    suspend fun loadGroups(): List<ContactGroup>

    /** Членство всех контактов: contactId → id групп. */
    suspend fun loadMembership(): Map<Long, Set<Long>>

    suspend fun loadContactGroupIds(contactId: Long): Set<Long>

    /** Создаёт локальную группу. */
    suspend fun createGroup(title: String): ContactGroup

    /** Создаёт группу в выбранном аккаунте; по умолчанию — локально. */
    suspend fun createGroup(
        title: String,
        account: ContactAccount,
    ): ContactGroup = createGroup(title)

    suspend fun renameGroup(
        groupId: Long,
        title: String,
    )

    suspend fun deleteGroup(groupId: Long)

    /** Добавляет контакты в группу; уже состоящие в ней пропускаются. */
    suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    )
}
