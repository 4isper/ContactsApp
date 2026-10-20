package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup

/** Группы (метки) контактов: чтение, управление и членство. */
interface GroupsRepository {
    suspend fun getGroups(): List<ContactGroup>

    /** Членство всех контактов: contactId → id групп. */
    suspend fun getMembership(): Map<Long, Set<Long>>

    suspend fun getContactGroups(contactId: Long): List<ContactGroup>

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
