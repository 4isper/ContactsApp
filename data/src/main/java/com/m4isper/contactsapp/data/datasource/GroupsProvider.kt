// Подавление осознанное: провайдер перечисляет все операции над группами.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.data.datasource.write.groups.GroupMergePlan
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup

/**
 * Провайдер групп: чтение и запись в провайдер контактов.
 * Методы не переключают потоки и не планируют операции — только выполняют их.
 */
interface GroupsProvider {
    fun loadGroups(): List<ContactGroup>

    /** Членство всех контактов: contactId → id групп. */
    fun loadMembership(): Map<Long, Set<Long>>

    fun loadContactGroupIds(contactId: Long): Set<Long>

    /** Создаёт группу. @return id новой группы. */
    fun createGroup(
        title: String,
        account: ContactAccount,
    ): Long

    fun renameGroup(
        groupId: Long,
        title: String,
    )

    fun deleteGroup(groupId: Long)

    /** Добавляет контакты в группу; уже состоящие в ней пропускаются. */
    fun addMembers(
        groupId: Long,
        contactIds: List<Long>,
    )

    /** Убирает контакты из группы. */
    fun removeMembers(
        groupId: Long,
        contactIds: List<Long>,
    )

    /** id контактов, состоящих в группе. */
    fun membersOf(groupId: Long): List<Long>

    /** raw-контакт каждого контакта: минимальный raw id. */
    fun rawContactsOf(contactIds: List<Long>): Map<Long, Long>

    /** Контакты, у которых эта группа уже есть — в любом из raw-контактов. */
    fun membersAlreadyIn(
        groupId: Long,
        contactIds: List<Long>,
    ): Set<Long>

    /**
     * Слияние групп одной операцией провайдера: перенос членства и удаление исходной
     * группы идут одним batch, поэтому применяются целиком или откатываются вместе.
     */
    fun mergeGroups(plan: GroupMergePlan)
}
