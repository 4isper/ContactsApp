package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity

/** Список контактов: порядок, плотность и избранные сверху. */
interface ListSettings {
    suspend fun setSortOrder(order: ContactSortOrder)

    suspend fun setListDensity(density: ListDensity)

    suspend fun setFavoritesFirst(enabled: Boolean)
}
