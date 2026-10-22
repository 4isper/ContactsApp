package com.m4isper.contactsapp.domain.model

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Хэш PIN-кода защиты приложения. Сам код не хранится: в настройках лежит
 * версионированный PBKDF2-хэш со случайной солью.
 */
fun hashAppLockPin(pin: String): String {
    val salt = ByteArray(PIN_SALT_BYTES).also(secureRandom::nextBytes)
    val hash = derivePinHash(pin, salt, PIN_HASH_ITERATIONS)
    return listOf(
        PIN_HASH_ALGORITHM,
        PIN_HASH_ITERATIONS.toString(),
        encode(salt),
        encode(hash),
    ).joinToString(PIN_HASH_SEPARATOR.toString())
}

/** Сверяет введённый PIN с сохранённым хэшем. */
fun verifyAppLockPin(
    pin: String,
    hash: String,
): Boolean {
    if (hash.isBlank()) return false
    // Совместимость с хэшами, сохранёнными до перехода на PBKDF2.
    return verifyVersionedPin(pin, hash) ?: verifyLegacyPin(pin, hash)
}

/**
 * Хэш сохранён в старом формате: после успешного ввода его нужно перезаписать.
 * Пустой хэш — нечего переписывать.
 */
fun needsAppLockHashMigration(hash: String): Boolean =
    hash.isNotBlank() && !hash.startsWith("$PIN_HASH_ALGORITHM$PIN_HASH_SEPARATOR")

private fun verifyVersionedPin(
    pin: String,
    hash: String,
): Boolean? {
    val parts = hash.split(PIN_HASH_SEPARATOR)
    return if (parts.size == PIN_HASH_PARTS && parts[0] == PIN_HASH_ALGORITHM) {
        parseVersionedPin(parts)?.let { parsed ->
            val actual = derivePinHash(pin, parsed.salt, parsed.iterations)
            MessageDigest.isEqual(actual, parsed.hash)
        } ?: false
    } else {
        null
    }
}

private fun parseVersionedPin(parts: List<String>): VersionedPinHash? {
    val iterations = parts[1].toIntOrNull()
    val salt = decode(parts[2])
    val hash = decode(parts[3])
    if (iterations == null || salt == null || hash == null) return null
    return if (iterations in 1..MAX_PIN_HASH_ITERATIONS && salt.isNotEmpty() && hash.isNotEmpty()) {
        VersionedPinHash(iterations, salt, hash)
    } else {
        null
    }
}

private data class VersionedPinHash(
    val iterations: Int,
    val salt: ByteArray,
    val hash: ByteArray,
)

private fun derivePinHash(
    pin: String,
    salt: ByteArray,
    iterations: Int,
): ByteArray {
    val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PIN_HASH_KEY_BITS)
    return try {
        SecretKeyFactory
            .getInstance(PIN_HASH_KEY_ALGORITHM)
            .generateSecret(spec)
            .encoded
    } finally {
        spec.clearPassword()
    }
}

/** Старый формат: SHA-256 от константной соли и PIN. Нужен только для миграции. */
private fun verifyLegacyPin(
    pin: String,
    hash: String,
): Boolean = MessageDigest.isEqual(legacyHash(pin).toByteArray(), hash.toByteArray())

private fun legacyHash(pin: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(LEGACY_PIN_SALT.toByteArray(Charsets.UTF_8) + pin.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

private fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)

private fun decode(value: String): ByteArray? = runCatching { Base64.getDecoder().decode(value) }.getOrNull()

private const val PIN_HASH_ALGORITHM = "pbkdf2-sha256"
private const val PIN_HASH_KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val PIN_HASH_KEY_BITS = 256
private const val PIN_SALT_BYTES = 16
private const val PIN_HASH_ITERATIONS = 210_000
private const val MAX_PIN_HASH_ITERATIONS = 1_000_000
private const val PIN_HASH_PARTS = 4
private const val PIN_HASH_SEPARATOR = '$'
private const val LEGACY_PIN_SALT = "contacts-app-lock-v1"

private val secureRandom = SecureRandom()
