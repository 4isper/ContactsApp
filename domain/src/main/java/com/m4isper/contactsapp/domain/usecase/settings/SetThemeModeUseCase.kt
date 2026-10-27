package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.repository.settings.AppearanceSettings

class SetThemeModeUseCase(
    private val repository: AppearanceSettings,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
