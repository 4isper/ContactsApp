package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject

@Inject
class SetSortOrderUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(order: ContactSortOrder) = repository.setSortOrder(order)
}
