package com.m4isper.contactsapp.data.repository

import android.provider.ContactsContract
import com.m4isper.contactsapp.data.datasource.ContactsWriterDataSource
import com.m4isper.contactsapp.data.datasource.PhoneContactsDataSource
import com.m4isper.contactsapp.data.datasource.mapPhoneKind
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ContactsRepositoryImpl(
    private val dataSource: PhoneContactsDataSource,
    private val writer: ContactsWriterDataSource,
) : ContactsRepository {
    override suspend fun getContacts(): List<ContactDetails> = dataSource.loadContacts()

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = dataSource.loadContact(contactId)

    override suspend fun getContactForm(contactId: Long): ContactForm? {
        val details = dataSource.loadContact(contactId) ?: return null
        val rows = writer.loadContactRows(contactId)
        return ContactForm(
            name = details.name,
            phones =
                rows.phones.map { row ->
                    EditablePhone(
                        dataId = row.dataId,
                        number = row.number,
                        kind = mapPhoneKind(row.type),
                        customLabel =
                            row.label?.takeIf {
                                row.type ==
                                    ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM &&
                                    it.isNotBlank()
                            },
                    )
                },
        )
    }

    override suspend fun createContact(form: ContactForm) = writer.createContact(form)

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        val rawContactId =
            requireNotNull(writer.loadRawContactId(contactId)) {
                "No raw contact for id=$contactId"
            }
        val rows = writer.loadContactRows(contactId)
        writer.updateContact(rawContactId, rows, form)
    }

    override suspend fun deleteContact(contactId: Long) = writer.deleteContact(contactId)
}
