package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.AppearanceSettings

class SetDynamicColorUseCase(
    private val repository: AppearanceSettings,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDynamicColor(enabled)
}
