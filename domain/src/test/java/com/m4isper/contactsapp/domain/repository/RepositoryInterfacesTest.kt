package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class FakeGroupsRepo : GroupsRepository {
    override suspend fun getGroups(): List<ContactGroup> = emptyList()

    override suspend fun getMembership(): Map<Long, Set<Long>> = emptyMap()

    override suspend fun getContactGroups(contactId: Long): List<ContactGroup> = emptyList()

    override suspend fun createGroup(title: String): ContactGroup = ContactGroup(id = 1L, title = title)

    override suspend fun createGroup(
        title: String,
        account: ContactAccount,
    ): ContactGroup = createGroup(title)

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ) = Unit

    override suspend fun deleteGroup(groupId: Long) = Unit

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) = Unit

    override suspend fun removeContactsFromGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) = Unit

    override suspend fun mergeGroups(
        sourceGroupId: Long,
        targetGroupId: Long,
    ) = Unit
}

private class FakeFavoritesRepo : FavoritesRepository {
    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit
}

class RepositoryInterfacesTest {
    @Test
    fun groupsRepositoryAllMethods() =
        runBlocking {
            val repo = FakeGroupsRepo()
            assertTrue(repo.getGroups().isEmpty())
            assertTrue(repo.getMembership().isEmpty())
            assertTrue(repo.getContactGroups(1L).isEmpty())
            assertEquals("t", repo.createGroup("t").title)
            repo.renameGroup(1L, "x")
            repo.deleteGroup(1L)
            repo.addContactsToGroup(1L, listOf(2L))
            repo.removeContactsFromGroup(1L, listOf(2L))
            repo.mergeGroups(1L, 2L)
            assertTrue(true)
        }

    @Test
    fun favoritesRepositoryAllMethods() =
        runBlocking {
            val repo = FakeFavoritesRepo()
            repo.toggleFavorite(5L)
            repo.removeFavorite(5L)
            assertTrue(true)
        }
}
