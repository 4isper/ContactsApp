package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.repository.ListSettings

class SetSortOrderUseCase(
    private val repository: ListSettings,
) {
    suspend operator fun invoke(order: ContactSortOrder) = repository.setSortOrder(order)
}
