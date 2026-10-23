package com.m4isper.contactsapp.data.datasource

import android.Manifest
import com.m4isper.contactsapp.data.datasource.write.groups.planGroupMerge
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Группы живут в провайдере контактов: созданная группа читается обратно, а
 * участники попадают в членство. Провайдер - в памяти, телефон не нужен.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupsDataSourceTest {
    private lateinit var provider: FakeContactsProvider
    private lateinit var dataSource: ContactsContractGroupsProvider

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.READ_CONTACTS)
        provider = Robolectric.setupContentProvider(FakeContactsProvider::class.java, AUTHORITY)
        dataSource = ContactsContractGroupsProvider(application)
    }

    @Test
    fun `the created group is read back`() =
        runTest {
            val id = dataSource.createGroup(title = "Work", account = ContactAccount.Local)

            val groups = dataSource.loadGroups()
            assertEquals(1, groups.size)
            assertEquals("Work", groups.single().title)
            assertEquals(id, groups.single().id)
        }

    @Test
    fun `the group is renamed`() =
        runTest {
            val id = dataSource.createGroup(title = "Old", account = ContactAccount.Local)

            dataSource.renameGroup(id, "New")

            assertEquals("New", dataSource.loadGroups().single().title)
        }

    @Test
    fun `the member of the group appears in the membership`() =
        runTest {
            val groupId = dataSource.createGroup(title = "Friends", account = ContactAccount.Local)
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID

            dataSource.addMembers(groupId, listOf(CONTACT_ID))

            val membership = dataSource.loadMembership()
            assertEquals(setOf(groupId), membership[CONTACT_ID].orEmpty())
        }

    @Test
    fun `the member is removed from the membership`() =
        runTest {
            val groupId = dataSource.createGroup(title = "Friends", account = ContactAccount.Local)
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID
            dataSource.addMembers(groupId, listOf(CONTACT_ID))

            dataSource.removeMembers(groupId, listOf(CONTACT_ID))

            assertTrue(dataSource.loadMembership().isEmpty())
        }

    @Test
    fun `the merge moves the members and deletes the source group`() =
        runTest {
            val source = dataSource.createGroup(title = "Source", account = ContactAccount.Local)
            val target = dataSource.createGroup(title = "Target", account = ContactAccount.Local)
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID
            dataSource.addMembers(source, listOf(CONTACT_ID))

            val plan =
                planGroupMerge(
                    sourceGroupId = source,
                    targetGroupId = target,
                    members = listOf(CONTACT_ID),
                    rawContacts = mapOf(CONTACT_ID to RAW_CONTACT_ID),
                    alreadyInTarget = emptySet(),
                )
            dataSource.mergeGroups(plan)

            val groups = dataSource.loadGroups()
            assertEquals(listOf("Target"), groups.map { it.title })
            assertEquals(setOf(target), dataSource.loadMembership()[CONTACT_ID].orEmpty())
        }

    @Test
    fun `the group of a contact is read by its id`() =
        runTest {
            val groupId = dataSource.createGroup(title = "Friends", account = ContactAccount.Local)
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID
            dataSource.addMembers(groupId, listOf(CONTACT_ID))

            assertEquals(setOf(groupId), dataSource.loadContactGroupIds(CONTACT_ID))
        }

    private companion object {
        const val AUTHORITY = "com.android.contacts"
        const val CONTACT_ID = 7L
        const val RAW_CONTACT_ID = 100L
    }
}
