package com.m4isper.contactsapp.domain.repository

/** Приватность: защита приложения PIN-кодом и скрытие окна в списке задач. */
interface PrivacySettings {
    /** Защита приложения: вкл/выкл и хэш PIN-кода. */
    suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    )

    /** Скрывать содержимое окна в недавних задачах. */
    suspend fun setSecureWindow(enabled: Boolean)
}
