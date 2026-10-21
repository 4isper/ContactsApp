package com.m4isper.contactsapp.domain.model

import java.security.MessageDigest

/**
 * Хэш PIN-кода защиты приложения. Сам код не хранится: в настройках лежит только
 * хэш с солью приложения, поэтому восстановить PIN из настроек нельзя.
 */
fun hashAppLockPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(PIN_SALT.toByteArray() + pin.toByteArray())
    return bytes.joinToString("") { byte -> "%02x".format(byte) }
}

/** Сверяет введённый PIN с сохранённым хэшем. */
fun verifyAppLockPin(
    pin: String,
    hash: String,
): Boolean = hash.isNotBlank() && hashAppLockPin(pin) == hash

/** Соль приложения: постоянная, чтобы хэш нельзя было подобрать по словарю. */
private const val PIN_SALT = "contacts-app-lock-v1"
