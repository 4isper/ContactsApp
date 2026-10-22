package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.repository.AppearanceSettings

class SetThemeModeUseCase(
    private val repository: AppearanceSettings,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
