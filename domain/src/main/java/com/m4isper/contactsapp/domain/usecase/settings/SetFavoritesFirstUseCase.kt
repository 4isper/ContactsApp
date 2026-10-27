package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.ListSettings

class SetFavoritesFirstUseCase(
    private val repository: ListSettings,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setFavoritesFirst(enabled)
}
