package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject

@Inject
class SetShowPhotosUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setShowPhotos(enabled)
}
