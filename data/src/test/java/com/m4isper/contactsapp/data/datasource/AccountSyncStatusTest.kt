package com.m4isper.contactsapp.data.datasource

import com.m4isper.contactsapp.domain.model.contact.AccountSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountSyncStatusTest {
    @Test
    fun `active sync wins over every other flag`() {
        assertEquals(
            AccountSyncStatus.SYNCING,
            resolveAccountSyncStatus(
                isSyncActive = true,
                isSyncPending = false,
                isSyncable = 0,
                syncAutomatically = false,
                masterSyncAutomatically = false,
                hasDirtyContacts = true,
            ),
        )
    }

    @Test
    fun `pending sync is reported as syncing`() {
        assertEquals(
            AccountSyncStatus.SYNCING,
            resolveAccountSyncStatus(
                isSyncActive = false,
                isSyncPending = true,
                isSyncable = 1,
                syncAutomatically = true,
                masterSyncAutomatically = true,
                hasDirtyContacts = false,
            ),
        )
    }

    @Test
    fun `unknown syncable state gives no status`() {
        assertNull(
            resolveAccountSyncStatus(
                isSyncActive = false,
                isSyncPending = false,
                isSyncable = -1,
                syncAutomatically = true,
                masterSyncAutomatically = true,
                hasDirtyContacts = false,
            ),
        )
    }

    @Test
    fun `disabled account sync is stale`() {
        assertEquals(
            AccountSyncStatus.STALE,
            resolveAccountSyncStatus(
                isSyncActive = false,
                isSyncPending = false,
                isSyncable = 1,
                syncAutomatically = false,
                masterSyncAutomatically = true,
                hasDirtyContacts = false,
            ),
        )
    }

    @Test
    fun `disabled global sync is stale`() {
        assertEquals(
            AccountSyncStatus.STALE,
            resolveAccountSyncStatus(
                isSyncActive = false,
                isSyncPending = false,
                isSyncable = 1,
                syncAutomatically = true,
                masterSyncAutomatically = false,
                hasDirtyContacts = false,
            ),
        )
    }

    @Test
    fun `account that cannot sync is stale`() {
        assertEquals(
            AccountSyncStatus.STALE,
            resolveAccountSyncStatus(
                isSyncActive = false,
                isSyncPending = false,
                isSyncable = 0,
                syncAutomatically = true,
                masterSyncAutomatically = true,
                hasDirtyContacts = false,
            ),
        )
    }

    @Test
    fun `unsent local changes are stale`() {
        assertEquals(
            AccountSyncStatus.STALE,
            resolveAccountSyncStatus(
                isSyncActive = false,
                isSyncPending = false,
                isSyncable = 1,
                syncAutomatically = true,
                masterSyncAutomatically = true,
                hasDirtyContacts = true,
            ),
        )
    }

    @Test
    fun `healthy account is synced`() {
        assertEquals(
            AccountSyncStatus.SYNCED,
            resolveAccountSyncStatus(
                isSyncActive = false,
                isSyncPending = false,
                isSyncable = 1,
                syncAutomatically = true,
                masterSyncAutomatically = true,
                hasDirtyContacts = false,
            ),
        )
    }
}
