package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.repository.settings.ListSettings

class SetSortOrderUseCase(
    private val repository: ListSettings,
) {
    suspend operator fun invoke(order: ContactSortOrder) = repository.setSortOrder(order)
}
