package com.m4isper.contactsapp.domain.repository.settings

/** Приватность: защита приложения PIN-кодом и скрытие окна в списке задач. */
interface PrivacySettings {
    /** Защита приложения: вкл/выкл, хэш PIN-кода и его длина (0 — неизвестна). */
    suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
        pinLength: Int,
    )

    /** Скрывать содержимое окна в недавних задачах. */
    suspend fun setSecureWindow(enabled: Boolean)

    /** Разблокировка по биометрии: действует, только если на устройстве есть биометрия. */
    suspend fun setAppLockBiometrics(enabled: Boolean)
}
