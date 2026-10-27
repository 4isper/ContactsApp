package com.m4isper.contactsapp.domain.usecase.settings

import com.m4isper.contactsapp.domain.repository.settings.LanguageSettings

class SetAppLanguageUseCase(
    private val repository: LanguageSettings,
) {
    suspend operator fun invoke(language: String) = repository.setAppLanguage(language)
}
