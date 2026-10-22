package com.m4isper.contactsapp.domain.security

/**
 * Хэширование PIN-кода защиты приложения. Криптография — инфраструктура,
 * домен знает только контракт: как выглядит хэш и как его проверить.
 */
interface PinHasher {
    /** Хэш PIN в версионированном формате; одинаковые PIN дают разные хэши. */
    fun hash(pin: String): String

    /** Сверяет введённый PIN с сохранённым хэшем. */
    fun verify(
        pin: String,
        hash: String,
    ): Boolean

    /** Хэш сохранён в старом формате: после успешного ввода его нужно перезаписать. */
    fun needsMigration(hash: String): Boolean
}
