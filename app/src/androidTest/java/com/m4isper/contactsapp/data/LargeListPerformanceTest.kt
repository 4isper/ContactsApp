package com.m4isper.contactsapp.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.m4isper.contactsapp.data.datasource.ContactsDataSourceImpl
import com.m4isper.contactsapp.domain.contacts.filterContacts
import com.m4isper.contactsapp.domain.model.ContactDetails
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Замер на большой базе: 5000 контактов. Проверяет, что страницы читаются
 * быстро, а весь список — в разумное время.
 */
@RunWith(AndroidJUnit4::class)
class LargeListPerformanceTest {
    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val dataSource by lazy { ContactsDataSourceImpl(context) }

    @Test
    fun five_thousand_contacts_load_page_by_page() =
        runBlocking {
            // Нагрузочный тест удлиняет прогон на минуты: включается флагом.
            assumeTrue(
                "Замер больших баз выключен: -Pandroid.testInstrumentationRunnerArguments.contacts.perf=true",
                InstrumentationRegistry.getArguments().getString("contacts.perf") == "true",
            )
            val created = createContacts(CONTACT_COUNT)
            try {
                awaitIndexed(created.size)
                val firstPage =
                    measureTimeMillis {
                        val page = dataSource.loadContactsPage(offset = 0, limit = PAGE_SIZE)
                        assertTrue("Страница пустая", page.isNotEmpty())
                        assertTrue("Страница больше лимита: ${page.size}", page.size <= PAGE_SIZE)
                    }
                val all =
                    measureTimeMillis {
                        var offset = 0
                        var loaded = 0
                        while (true) {
                            val page = dataSource.loadContactsPage(offset = offset, limit = PAGE_SIZE)
                            if (page.isEmpty()) break
                            loaded += page.size
                            offset += page.size
                        }
                        assertTrue("Загружено меньше, чем создано: $loaded", loaded >= created.size)
                    }
                val list = loadAll()
                val search =
                    measureTimeMillis {
                        val found = filterContacts(list, "Perf0100", favoritesOnly = false, favorites = emptySet())
                        assertTrue("Поиск ничего не нашёл", found.isNotEmpty())
                    }
                println(
                    "PERF first_page_ms=$firstPage all_pages_ms=$all search_ms=$search contacts=${created.size}",
                )
                assertTrue("Первая страница слишком долго: $firstPage мс", firstPage < FIRST_PAGE_LIMIT_MS)
                assertTrue("Весь список слишком долго: $all мс", all < ALL_PAGES_LIMIT_MS)
                assertTrue("Поиск слишком долгий: $search мс", search < SEARCH_LIMIT_MS)
            } finally {
                created.forEach { deleteContact(it) }
            }
        }

    /** Весь список контактов страницами. */
    private suspend fun loadAll(): List<ContactDetails> {
        val all = mutableListOf<ContactDetails>()
        var offset = 0
        while (true) {
            val page = dataSource.loadContactsPage(offset = offset, limit = PAGE_SIZE)
            if (page.isEmpty()) break
            all += page
            offset += page.size
        }
        return all
    }

    /** Провайдер индексирует контакты асинхронно: ждём, пока они появятся в выборке. */
    private suspend fun awaitIndexed(expected: Int) {
        val deadline = System.currentTimeMillis() + INDEX_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            var offset = 0
            var loaded = 0
            while (true) {
                val page = dataSource.loadContactsPage(offset = offset, limit = PAGE_SIZE)
                if (page.isEmpty()) break
                loaded += page.size
                offset += page.size
            }
            if (loaded >= expected) return
            kotlinx.coroutines.delay(500)
        }
    }

    private fun createContacts(count: Int): List<Long> {
        val ids = mutableListOf<Long>()
        repeat(count) { index ->
            val rawValues =
                ContentValues().apply {
                    putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
                }
            val rawUri =
                requireNotNull(
                    context.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, rawValues),
                )
            val rawId = requireNotNull(rawUri.lastPathSegment).toLong()
            context.contentResolver.insert(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    )
                    put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, "Perf%04d".format(index))
                    put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "Contact")
                    put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "Perf%04d Contact".format(index))
                },
            )
            ids += rawId
        }
        return ids
    }

    private fun deleteContact(rawId: Long) {
        context.contentResolver.delete(
            ContactsContract.RawContacts.CONTENT_URI,
            "${ContactsContract.RawContacts._ID}=?",
            arrayOf(rawId.toString()),
        )
    }

    private companion object {
        const val CONTACT_COUNT = 5000
        const val PAGE_SIZE = 200
        const val FIRST_PAGE_LIMIT_MS = 5_000L
        const val ALL_PAGES_LIMIT_MS = 120_000L
        const val INDEX_TIMEOUT_MILLIS = 120_000L
        const val SEARCH_LIMIT_MS = 1_500L
    }
}
