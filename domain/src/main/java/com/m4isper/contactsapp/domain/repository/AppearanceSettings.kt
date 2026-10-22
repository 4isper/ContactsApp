package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ThemeMode

/** Внешний вид: тема, динамические цвета и фотографии контактов. */
interface AppearanceSettings {
    suspend fun setThemeMode(mode: ThemeMode)

    /** Динамические цвета вместо фирменной палитры. */
    suspend fun setDynamicColor(enabled: Boolean)

    /** Показывать фотографии контактов. */
    suspend fun setShowPhotos(enabled: Boolean)
}
