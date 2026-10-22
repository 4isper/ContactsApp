package com.m4isper.contactsapp.data.datasource

import android.Manifest
import kotlinx.coroutines.flow.first
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
 * Избранное живёт в провайдере контактов: звезда должна появиться строкой данных
 * и исчезнуть при снятии. Проверяем без телефона - провайдер в памяти.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoritesDataSourceTest {
    private lateinit var provider: FakeContactsProvider
    private lateinit var dataSource: FavoritesDataSourceImpl

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(application).grantPermissions(Manifest.permission.READ_CONTACTS)
        provider = Robolectric.setupContentProvider(FakeContactsProvider::class.java, AUTHORITY)
        dataSource = FavoritesDataSourceImpl(application)
    }

    @Test
    fun `the favorite becomes a starred row of the contact`() =
        runTest {
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID

            dataSource.toggle(CONTACT_ID)

            val rows = provider.dataRows.filter { it.mimeType == STARRED_MIME_TYPE }
            assertEquals(1, rows.size)
            assertEquals(RAW_CONTACT_ID, rows.single().rawContactId)
        }

    @Test
    fun `the favorites of the provider are read back`() =
        runTest {
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID
            dataSource.toggle(CONTACT_ID)

            assertEquals(setOf(CONTACT_ID), dataSource.favorites.first())
        }

    @Test
    fun `toggling twice removes the star`() =
        runTest {
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID
            dataSource.toggle(CONTACT_ID)

            dataSource.toggle(CONTACT_ID)

            assertTrue(provider.dataRows.none { it.mimeType == STARRED_MIME_TYPE })
            assertEquals(emptySet<Long>(), dataSource.favorites.first())
        }

    @Test
    fun `the star of one contact does not touch the other`() =
        runTest {
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID
            provider.rawContacts[OTHER_CONTACT_ID] = OTHER_RAW_CONTACT_ID
            dataSource.toggle(CONTACT_ID)

            dataSource.toggle(OTHER_CONTACT_ID)

            val starred = provider.dataRows.filter { it.mimeType == STARRED_MIME_TYPE }
            assertEquals(2, starred.size)
        }

    @Test
    fun `removing the favorite clears the row of the provider`() =
        runTest {
            provider.rawContacts[CONTACT_ID] = RAW_CONTACT_ID
            dataSource.toggle(CONTACT_ID)

            dataSource.remove(CONTACT_ID)

            assertTrue(provider.dataRows.none { it.mimeType == STARRED_MIME_TYPE })
        }

    @Test
    fun `a contact without a raw row is not starred`() =
        runTest {
            dataSource.toggle(CONTACT_ID)

            assertTrue(provider.dataRows.isEmpty())
        }

    private companion object {
        const val AUTHORITY = "com.android.contacts"
        const val STARRED_MIME_TYPE = "vnd.android.cursor.item/starred"
        const val CONTACT_ID = 7L
        const val RAW_CONTACT_ID = 100L
        const val OTHER_CONTACT_ID = 8L
        const val OTHER_RAW_CONTACT_ID = 200L
    }
}
