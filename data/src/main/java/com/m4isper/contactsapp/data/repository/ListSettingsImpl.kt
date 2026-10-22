package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.SettingsDataSource
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.repository.AccountSettings
import com.m4isper.contactsapp.domain.repository.ListSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/** Список контактов и аккаунты в локальном хранилище. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ListSettings>())
@ContributesBinding(AppScope::class, binding = binding<AccountSettings>())
class ListSettingsImpl(
    private val dataSource: SettingsDataSource,
) : ListSettings,
    AccountSettings {
    override suspend fun setSortOrder(order: ContactSortOrder) = dataSource.setSortOrder(order.name)

    override suspend fun setListDensity(density: ListDensity) = dataSource.setListDensity(density.name)

    override suspend fun setFavoritesFirst(enabled: Boolean) = dataSource.setFavoritesFirst(enabled)

    override suspend fun setLastContactAccount(account: ContactAccount) = dataSource.setLastContactAccount(account)

    override suspend fun setDefaultContactAccount(account: ContactAccount) =
        dataSource.setDefaultContactAccount(account)
}
