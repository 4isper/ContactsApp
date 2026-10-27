package com.m4isper.contactsapp.data

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.data.datasource.ContactsContractGroupsProvider
import com.m4isper.contactsapp.data.datasource.GroupsDataSourceImpl
import com.m4isper.contactsapp.data.repository.GroupsRepositoryImpl
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Группы живут в провайдере контактов: созданная группа должна читаться обратно,
 * а её участники - попадать в членство.
 */
@RunWith(AndroidJUnit4::class)
class GroupsDataSourceTest {
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val repository by lazy {
        GroupsRepositoryImpl(GroupsDataSourceImpl(ContactsContractGroupsProvider(context)))
    }

    @Test
    fun created_group_is_read_back() =
        runBlocking {
            val group = repository.createGroup("Work")

            try {
                val groups = repository.getGroups()
                assertTrue(groups.any { it.id == group.id && it.title == "Work" })
            } finally {
                repository.deleteGroup(group.id)
            }
        }

    @Test
    fun group_is_renamed() =
        runBlocking {
            val group = repository.createGroup("Old")

            try {
                repository.renameGroup(group.id, "New")

                val groups = repository.getGroups()
                assertEquals("New", groups.first { it.id == group.id }.title)
            } finally {
                repository.deleteGroup(group.id)
            }
        }

    @Test
    fun member_of_the_group_appears_in_the_membership() =
        runBlocking {
            val group = repository.createGroup("Friends")
            val contactId = createContact()

            try {
                repository.addContactsToGroup(group.id, listOf(contactId))

                val membership = repository.getMembership()
                assertTrue(group.id in membership[contactId].orEmpty())
            } finally {
                repository.removeContactsFromGroup(group.id, listOf(contactId))
                repository.deleteGroup(group.id)
                deleteContact(contactId)
            }
        }

    private fun createContact(): Long {
        val uri =
            requireNotNull(
                context.contentResolver.insert(
                    ContactsContract.RawContacts.CONTENT_URI,
                    android.content.ContentValues().apply {
                        put(ContactsContract.RawContacts.ACCOUNT_TYPE, ContactAccount.Local.type)
                        put(ContactsContract.RawContacts.ACCOUNT_NAME, ContactAccount.Local.name)
                    },
                ),
            )
        return uri.lastPathSegment?.toLong() ?: error("contact not created")
    }

    private fun deleteContact(contactId: Long) {
        context.contentResolver.delete(
            ContactsContract.RawContacts.CONTENT_URI,
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
        )
    }
}
