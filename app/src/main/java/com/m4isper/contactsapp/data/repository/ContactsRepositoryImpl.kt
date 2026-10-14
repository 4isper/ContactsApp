package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.PhoneContactsDataSource
import com.m4isper.contactsapp.domain.model.Contact
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
) : ContactsRepository {
    override suspend fun getContacts(): List<Contact> = dataSource.loadContacts()
}
