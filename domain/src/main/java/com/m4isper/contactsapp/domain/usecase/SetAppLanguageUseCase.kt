package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.repository.LanguageSettings

class SetAppLanguageUseCase(
    private val repository: LanguageSettings,
) {
    suspend operator fun invoke(language: String) = repository.setAppLanguage(language)
}
