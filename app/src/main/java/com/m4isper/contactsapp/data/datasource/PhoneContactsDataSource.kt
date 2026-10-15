package com.m4isper.contactsapp.data.datasource

import android.content.Context
import android.provider.ContactsContract
import com.m4isper.contactsapp.di.AppContext
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CONTACT_PROJECTION = arrayOf(
    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
    ContactsContract.CommonDataKinds.Phone.NUMBER,
    ContactsContract.CommonDataKinds.Phone.TYPE,
    ContactsContract.CommonDataKinds.Phone.LABEL,
)

private const val CONTACT_SORT_ORDER =
    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

/** Сырая строка из [ContactsContract] — одна на каждый номер. */
internal data class PhoneRow(
    val contactId: Long,
    val name: String,
    val phone: String,
    val type: Int,
    val label: String?,
)

internal fun mapPhoneKind(type: Int): PhoneKind = when (type) {
    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> PhoneKind.MOBILE
    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> PhoneKind.HOME
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
    ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,
    -> PhoneKind.WORK
    else -> PhoneKind.OTHER
}

/** Чистая агрегация строк в контакты по CONTACT_ID. */
internal fun aggregateContacts(rows: List<PhoneRow>): List<ContactDetails> =
    rows
        .filter { it.name.isNotBlank() && it.phone.isNotBlank() }
        .groupBy { it.contactId }
        .mapNotNull { (contactId, group) ->
            val name = group.firstOrNull { it.name.isNotBlank() }?.name ?: return@mapNotNull null
            val phones = group
                .map {
                    PhoneNumber(
                        number = it.phone,
                        kind = mapPhoneKind(it.type),
                        customLabel = it.label?.takeIf { label ->
                            it.type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM &&
                                label.isNotBlank()
                        },
                    )
                }
                .distinctBy { it.number }
            if (phones.isEmpty()) return@mapNotNull null
            ContactDetails(contactId = contactId, name = name, phones = phones)
        }
        .sortedBy { it.name }

/**
 * Единственное место, знающее про [ContactsContract].
 * Работает на IO-диспетчере, наружу отдаёт чистые [ContactDetails].
 */
@Inject
@SingleIn(AppScope::class)
class PhoneContactsDataSource(
    @AppContext private val context: Context,
) {
    suspend fun loadContacts(): List<ContactDetails> = withContext(Dispatchers.IO) {
        aggregateContacts(queryRows(selection = null, args = null))
    }

    suspend fun loadContact(contactId: Long): ContactDetails? = withContext(Dispatchers.IO) {
        aggregateContacts(
            queryRows(
                selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                args = arrayOf(contactId.toString()),
            ),
        ).firstOrNull()
    }

    private fun queryRows(selection: String?, args: Array<String>?): List<PhoneRow> {
        val rows = mutableListOf<PhoneRow>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            CONTACT_PROJECTION,
            selection,
            args,
            CONTACT_SORT_ORDER,
        )

        cursor?.use {
            val idIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (it.moveToNext()) {
                rows.add(
                    PhoneRow(
                        contactId = it.getLong(idIndex),
                        name = it.getString(nameIndex)?.trim().orEmpty(),
                        phone = it.getString(phoneIndex)?.trim().orEmpty(),
                        type = it.getInt(typeIndex),
                        label = it.getString(labelIndex),
                    ),
                )
            }
        }

        return rows
    }
}
