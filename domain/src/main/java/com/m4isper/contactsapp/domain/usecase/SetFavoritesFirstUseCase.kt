package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.ListSettings
import dev.zacsweers.metro.Inject

@Inject
class SetFavoritesFirstUseCase(
    private val repository: ListSettings,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setFavoritesFirst(enabled)
}
