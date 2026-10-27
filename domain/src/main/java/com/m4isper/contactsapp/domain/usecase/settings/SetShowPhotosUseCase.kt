package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.AppearanceSettings

class SetShowPhotosUseCase(
    private val repository: AppearanceSettings,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setShowPhotos(enabled)
}
