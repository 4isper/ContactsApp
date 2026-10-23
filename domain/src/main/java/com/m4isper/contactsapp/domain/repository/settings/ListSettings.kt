package com.m4isper.contactsapp.domain.repository.settings

import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity

/** Список контактов: порядок, плотность и избранные сверху. */
interface ListSettings {
    suspend fun setSortOrder(order: ContactSortOrder)

    suspend fun setListDensity(density: ListDensity)

    suspend fun setFavoritesFirst(enabled: Boolean)
}
