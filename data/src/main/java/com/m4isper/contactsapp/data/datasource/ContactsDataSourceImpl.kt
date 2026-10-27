package com.m4isper.contactsapp.data.datasource

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.data.datasource.read.CONTACT_DATA_PROJECTION
import com.m4isper.contactsapp.data.datasource.read.ContactHeaderRow
import com.m4isper.contactsapp.data.datasource.read.DataColumns
import com.m4isper.contactsapp.data.datasource.read.DataRow
import com.m4isper.contactsapp.data.datasource.read.aggregateContacts
import com.m4isper.contactsapp.data.datasource.read.buildContactDetails
import com.m4isper.contactsapp.data.datasource.write.contacts.readContactPhotoBytes
import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.AccountSyncStatus
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CONTACT_PROJECTION =
    arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
        ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
        ContactsContractLegacy.LAST_TIME_CONTACTED,
    )

private const val CONTACT_SORT_ORDER =
    "${ContactsContract.Contacts.DISPLAY_NAME} ASC, ${ContactsContract.Contacts._ID} ASC"

/**
 * Контакты без имени в список не попадают. Фильтр уходит в провайдер, а не в
 * Kotlin: иначе страница считается по «сырым» заголовкам, а показывается меньше
 * контактов — и постраничная загрузка теряет часть списка.
 */
private const val VISIBLE_CONTACTS_SELECTION =
    "LENGTH(TRIM(${ContactsContract.Contacts.DISPLAY_NAME})) > 0"

/** Типы данных, которые нужны карточке контакта. */
private val DATA_MIMETYPES =
    arrayOf(
        Phone.CONTENT_ITEM_TYPE,
        Email.CONTENT_ITEM_TYPE,
        StructuredName.CONTENT_ITEM_TYPE,
        Nickname.CONTENT_ITEM_TYPE,
        Relation.CONTENT_ITEM_TYPE,
        Organization.CONTENT_ITEM_TYPE,
        StructuredPostal.CONTENT_ITEM_TYPE,
        Website.CONTENT_ITEM_TYPE,
        Note.CONTENT_ITEM_TYPE,
        ContactsContractLegacy.Sip.CONTENT_ITEM_TYPE,
        Event.CONTENT_ITEM_TYPE,
        ContactsContractLegacy.Im.CONTENT_ITEM_TYPE,
    )

/** Сколько id контактов уходит в один запрос к провайдеру. */
private const val IDS_PER_QUERY = 500

/**
 * Единственное место чтения [ContactsContract].
 * Список читается по таблице Contacts (видны и контакты без номера),
 * детали — по data-строкам контакта.
 */
@Suppress("TooManyFunctions") // Осознанно: у читателя контактов есть своя функция на каждый запрос к провайдеру.
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContactsDataSourceImpl(
    private val context: Context,
) : ContactsDataSource {
    override suspend fun loadContacts(): List<ContactDetails> =
        withContext(Dispatchers.IO) {
            val headers = queryHeaders(selection = VISIBLE_CONTACTS_SELECTION, args = null)
            val rows = queryRowsOf(headers.map { it.contactId })
            aggregateContacts(headers, rows, queryContactAccounts())
        }

    override suspend fun loadContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> =
        withContext(Dispatchers.IO) {
            val headers =
                queryHeaders(
                    selection = VISIBLE_CONTACTS_SELECTION,
                    args = null,
                    sortOrder = pageSortOrder(offset, limit),
                )
            if (headers.isEmpty()) return@withContext emptyList()
            val rows = queryRowsOf(headers.map { it.contactId })
            aggregateContacts(headers, rows, queryContactAccounts())
        }

    override suspend fun loadContact(contactId: Long): ContactDetails? =
        withContext(Dispatchers.IO) {
            val header =
                queryHeaders(
                    selection = "${ContactsContract.Contacts._ID} = ?",
                    args = arrayOf(contactId.toString()),
                ).firstOrNull() ?: return@withContext null
            val rows =
                queryData(
                    selection = "${ContactsContract.Data.CONTACT_ID} = ?",
                    args = arrayOf(contactId.toString()),
                )
            buildContactDetails(header, rows, account = queryContactAccount(context, contactId))
        }

    override suspend fun loadContactAccount(contactId: Long): ContactAccount =
        withContext(Dispatchers.IO) {
            queryContactAccount(context, contactId)
        }

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? =
        withContext(Dispatchers.IO) {
            readContactPhotoBytes(context.contentResolver, contactId)
        }

    override suspend fun loadProfileContactId(): Long? =
        withContext(Dispatchers.IO) {
            val cursor =
                context.contentResolver.query(
                    ContactsContract.Profile.CONTENT_URI,
                    arrayOf(ContactsContract.Profile._ID),
                    null,
                    null,
                    null,
                )
            cursor?.use {
                if (it.moveToFirst()) it.getLong(0) else null
            }
        }

    override suspend fun getContactAccounts(): List<ContactAccount> =
        withContext(Dispatchers.IO) {
            val contactTypes = contactSyncAccountTypes()
            val accounts = linkedMapOf<String, ContactAccount>()
            addAccountManagerAccounts(contactTypes, accounts)
            addSettingsAccounts(contactTypes, accounts)
            if (accounts.isEmpty()) {
                addRawContactAccounts(contactTypes, accounts)
            }
            listOf(ContactAccount.Local) +
                accounts.values.sortedWith(
                    Comparator { first, second ->
                        val byName =
                            String.CASE_INSENSITIVE_ORDER.compare(
                                first.name.orEmpty(),
                                second.name.orEmpty(),
                            )
                        if (byName != 0) {
                            byName
                        } else {
                            String.CASE_INSENSITIVE_ORDER.compare(
                                first.type.orEmpty(),
                                second.type.orEmpty(),
                            )
                        }
                    },
                )
        }

    override suspend fun getAccountSyncInfos(): List<AccountSyncInfo> =
        withContext(Dispatchers.IO) {
            getContactAccounts()
                .asSequence()
                .filterNot { it.isLocal }
                .mapNotNull { account ->
                    val name = account.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val type = account.type?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val androidAccount = Account(name, type)

                    // Публичный Android API не отдаёт время последней успешной
                    // синхронизации. Не подставляем System.currentTimeMillis():
                    // лучше показать только реальный статус, чем выдуманное
                    // «только что».
                    val status = queryAccountSyncStatus(androidAccount) ?: return@mapNotNull null
                    AccountSyncInfo(
                        account = account,
                        syncStatus = status,
                        lastSyncTimeMs = null,
                    )
                }.toList()
        }

    /**
     * Статус по данным, которые действительно доступны приложению:
     * активная/ожидающая синхронизация, настройки автосинхронизации и
     * неотправленные изменения. Результат последней синхронизации Android
     * публично не сообщает, поэтому [AccountSyncStatus.ERROR] здесь не
     * синтезируется.
     */
    private fun queryAccountSyncStatus(account: Account): AccountSyncStatus? =
        runCatching {
            val authority = ContactsContract.AUTHORITY
            val isActive = ContentResolver.isSyncActive(account, authority)
            val isPending = ContentResolver.isSyncPending(account, authority)
            val isSyncable = ContentResolver.getIsSyncable(account, authority)
            val syncAutomatically = ContentResolver.getSyncAutomatically(account, authority)
            val masterSyncAutomatically = ContentResolver.getMasterSyncAutomatically()
            val hasDirtyContacts =
                if (isActive || isPending) {
                    false
                } else {
                    hasDirtyContacts(account)
                }

            resolveAccountSyncStatus(
                isSyncActive = isActive,
                isSyncPending = isPending,
                isSyncable = isSyncable,
                syncAutomatically = syncAutomatically,
                masterSyncAutomatically = masterSyncAutomatically,
                hasDirtyContacts = hasDirtyContacts,
            )
        }.getOrNull()

    private fun hasDirtyContacts(account: Account): Boolean =
        context.contentResolver
            .query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND " +
                    "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ? AND " +
                    "${ContactsContract.RawContacts.DIRTY} = 1",
                arrayOf(account.name, account.type),
                null,
            )?.use { cursor -> cursor.count > 0 } ?: false

    private fun addAccountManagerAccounts(
        contactTypes: Set<String>,
        result: MutableMap<String, ContactAccount>,
    ) {
        val accountManager = AccountManager.get(context)
        contactTypes.forEach { type ->
            val accounts =
                try {
                    accountManager.getAccountsByType(type)
                } catch (_: SecurityException) {
                    emptyArray<Account>()
                }
            accounts.forEach { addAccount(result, it.name, it.type) }
        }
    }

    private fun addSettingsAccounts(
        contactTypes: Set<String>,
        result: MutableMap<String, ContactAccount>,
    ) {
        queryProviderAccounts(
            uri = ContactsContract.Settings.CONTENT_URI,
            nameColumn = ContactsContract.Settings.ACCOUNT_NAME,
            typeColumn = ContactsContract.Settings.ACCOUNT_TYPE,
            contactTypes = contactTypes,
            result = result,
        )
    }

    /** Запасной источник, если настройки аккаунтов недоступны: уже существующие raw contacts. */
    private fun addRawContactAccounts(
        contactTypes: Set<String>,
        result: MutableMap<String, ContactAccount>,
    ) {
        queryProviderAccounts(
            uri = ContactsContract.RawContacts.CONTENT_URI,
            nameColumn = ContactsContract.RawContacts.ACCOUNT_NAME,
            typeColumn = ContactsContract.RawContacts.ACCOUNT_TYPE,
            contactTypes = contactTypes,
            result = result,
        )
    }

    private fun queryProviderAccounts(
        uri: Uri,
        nameColumn: String,
        typeColumn: String,
        contactTypes: Set<String>,
        result: MutableMap<String, ContactAccount>,
    ) {
        val cursor =
            try {
                context.contentResolver.query(uri, arrayOf(nameColumn, typeColumn), null, null, null)
            } catch (_: SecurityException) {
                null
            }
        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(nameColumn)
            val typeIndex = it.getColumnIndexOrThrow(typeColumn)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val type = it.getString(typeIndex)
                if (type != null && (contactTypes.isEmpty() || type in contactTypes)) {
                    addAccount(result, name, type)
                }
            }
        }
    }

    private fun addAccount(
        result: MutableMap<String, ContactAccount>,
        name: String?,
        type: String?,
    ) {
        if (name.isNullOrBlank() || type.isNullOrBlank()) return
        result.putIfAbsent("$type\u0000$name", ContactAccount(name = name, type = type))
    }

    /**
     * Порядок и разбор страницы. Провайдер понимает «LIMIT n OFFSET m» в sortOrder,
     * но пустой OFFSET отбрасывает выборку — указываем его только со второго раза.
     */
    private fun pageSortOrder(
        offset: Int,
        limit: Int,
    ): String =
        if (offset > 0) {
            "$CONTACT_SORT_ORDER LIMIT $limit OFFSET $offset"
        } else {
            "$CONTACT_SORT_ORDER LIMIT $limit"
        }

    private fun queryHeaders(
        selection: String?,
        args: Array<String>?,
        sortOrder: String = CONTACT_SORT_ORDER,
    ): List<ContactHeaderRow> {
        val headers = mutableListOf<ContactHeaderRow>()
        val cursor =
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACT_PROJECTION,
                selection,
                args,
                sortOrder,
            )
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            val updatedIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
            val lastContactedIndex = it.getColumnIndexOrThrow(ContactsContractLegacy.LAST_TIME_CONTACTED)
            while (it.moveToNext()) {
                headers +=
                    ContactHeaderRow(
                        contactId = it.getLong(idIndex),
                        name = it.getString(nameIndex)?.trim().orEmpty(),
                        photoUri = it.getString(photoIndex),
                        lastUpdated = if (it.isNull(updatedIndex)) null else it.getLong(updatedIndex),
                        lastContacted =
                            if (it.isNull(lastContactedIndex)) null else it.getLong(lastContactedIndex),
                    )
            }
        }
        return headers
    }

    /**
     * Строки данных (телефоны, почта, имена) только для указанных контактов:
     * полная выборка Data на каждую страницу читала бы всю базу целиком.
     */
    private fun queryRowsOf(contactIds: List<Long>): List<DataRow> {
        if (contactIds.isEmpty()) return emptyList()
        val rows = mutableListOf<DataRow>()
        contactIds.chunked(IDS_PER_QUERY).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            rows +=
                queryData(
                    selection =
                        "${ContactsContract.Data.CONTACT_ID} IN ($placeholders) AND " +
                            "${ContactsContract.Data.MIMETYPE} IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    args = chunk.map(Long::toString).toTypedArray() + DATA_MIMETYPES,
                )
        }
        return rows
    }

    private fun queryData(
        selection: String?,
        args: Array<String>?,
    ): List<DataRow> {
        val rows = mutableListOf<DataRow>()
        val cursor =
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                CONTACT_DATA_PROJECTION,
                selection,
                args,
                "${ContactsContract.Data.CONTACT_ID} ASC, ${ContactsContract.Data._ID} ASC",
            )
        cursor?.use {
            val columns = DataColumns(it)
            while (it.moveToNext()) {
                rows +=
                    DataRow(
                        contactId = it.getLong(columns.contactId),
                        mimetype = it.getString(columns.mime).orEmpty(),
                        data1 = it.getString(columns.data1),
                        data2 = it.getString(columns.data2),
                        data3 = it.getString(columns.data3),
                        data4 = it.getString(columns.data4),
                        data5 = it.getString(columns.data5),
                        data6 = it.getString(columns.data6),
                        data7 = it.getString(columns.data7),
                        data8 = it.getString(columns.data8),
                        data9 = it.getString(columns.data9),
                        data10 = it.getString(columns.data10),
                        isPrimary = it.getInt(columns.isPrimary) == 1,
                    )
            }
        }
        return rows
    }

    /** Аккаунт первого raw contact для каждого контакта — для фильтра по аккаунтам в списке. */
    private fun queryContactAccounts(): Map<Long, ContactAccount> {
        val accounts = mutableMapOf<Long, ContactAccount>()
        context.contentResolver
            .query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.CONTACT_ID,
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts.ACCOUNT_TYPE,
                ),
                "${ContactsContract.RawContacts.DELETED} = 0",
                null,
                "${ContactsContract.RawContacts.CONTACT_ID} ASC, ${ContactsContract.RawContacts._ID} ASC",
            )?.use { cursor ->
                val contactIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
                val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(contactIndex)
                    if (contactId <= 0 || contactId in accounts) continue
                    accounts[contactId] =
                        ContactAccount(
                            name = cursor.getString(nameIndex),
                            type = cursor.getString(typeIndex),
                        )
                }
            }
        return accounts
    }
}

/** Типы аккаунтов, у которых есть sync adapter для контактов с поддержкой записи. */
private fun contactSyncAccountTypes(): Set<String> =
    ContentResolver
        .getSyncAdapterTypes()
        .filter { it.authority == ContactsContract.AUTHORITY && it.supportsUploading() }
        .mapTo(mutableSetOf()) { it.accountType }

/**
 * Чистое правило статуса: его можно проверить без Android runtime.
 * `null` означает «статус неизвестен», а не «синхронизировано».
 */
internal fun resolveAccountSyncStatus(
    isSyncActive: Boolean,
    isSyncPending: Boolean,
    isSyncable: Int,
    syncAutomatically: Boolean,
    masterSyncAutomatically: Boolean,
    hasDirtyContacts: Boolean,
): AccountSyncStatus? =
    when {
        isSyncActive || isSyncPending -> AccountSyncStatus.SYNCING
        isSyncable < 0 -> null
        isSyncable == 0 || !syncAutomatically || !masterSyncAutomatically -> AccountSyncStatus.STALE
        hasDirtyContacts -> AccountSyncStatus.STALE
        else -> AccountSyncStatus.SYNCED
    }

/**
 * Аккаунт целевого raw contact — минимальный raw id, как в редакторе.
 * Локальные raw contacts хранят null в account name/type.
 */
private fun queryContactAccount(
    context: Context,
    contactId: Long,
): ContactAccount =
    context.contentResolver
        .query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
            ),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            "${ContactsContract.RawContacts._ID} ASC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
                val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
                ContactAccount(
                    name = cursor.getString(nameIndex),
                    type = cursor.getString(typeIndex),
                )
            } else {
                ContactAccount.Local
            }
        } ?: ContactAccount.Local
