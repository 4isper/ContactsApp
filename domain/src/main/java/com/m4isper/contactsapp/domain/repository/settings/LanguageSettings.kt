package com.m4isper.contactsapp.domain.repository.settings

/** Язык приложения: пусто — язык системы. */
interface LanguageSettings {
    suspend fun setAppLanguage(language: String)
}
