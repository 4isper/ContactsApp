package com.m4isper.contactsapp.data.datasource

import android.content.Context
import android.provider.ContactsContract
import com.m4isper.contactsapp.di.AppContext
import com.m4isper.contactsapp.domain.model.Contact
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CONTACT_PROJECTION = arrayOf(
    ContactsContract.CommonDataKinds.Phone._ID,
    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
    ContactsContract.CommonDataKinds.Phone.NUMBER,
)

private const val CONTACT_SORT_ORDER =
    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

/**
 * Единственное место, знающее про [ContactsContract].
 * Работает на IO-диспетчере, наружу отдаёт чистые [Contact].
 */
@Inject
@SingleIn(AppScope::class)
class PhoneContactsDataSource(
    @AppContext private val context: Context,
) {
    suspend fun loadContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            CONTACT_PROJECTION,
            null,
            null,
            CONTACT_SORT_ORDER,
        )

        cursor?.use {
            val idIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)
            val nameIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex =
                it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)?.trim().orEmpty()
                val phone = it.getString(phoneIndex)?.trim().orEmpty()

                if (name.isBlank() || phone.isBlank()) continue

                contacts.add(Contact(id = id, name = name, phone = phone))
            }
        }

        contacts.distinctBy { it.id to it.phone }
    }
}
