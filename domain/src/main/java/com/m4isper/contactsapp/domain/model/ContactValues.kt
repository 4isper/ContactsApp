package com.m4isper.contactsapp.domain.model

import java.net.IDN
import java.net.URI
import java.util.Locale

/**
 * Значения формы, которые пишутся в провайдер: адрес почты, телефон и сайт.
 * Правила нормализации и проверки живут здесь, редактор и импорт берут их одни.
 *
 * Адрес почты: локальная часть сохраняется, домен приводится к нижнему регистру.
 * Домен должен состоять из меток, а его последняя метка - из букв.
 */
class Email private constructor(
    val value: String,
) {
    override fun equals(other: Any?): Boolean = other is Email && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        /** Нормализует адрес и проверяет его; null, если адрес негодный. */
        fun of(raw: String): Email? {
            val trimmed = raw.trim()
            val parts = trimmed.split('@')
            val local = parts.getOrNull(0).orEmpty()
            val domain = parts.getOrNull(1)?.lowercase(Locale.ROOT).orEmpty()
            return when {
                trimmed.isEmpty() || trimmed.any { it.isWhitespace() } -> null
                parts.size != 2 -> null
                local.isEmpty() || !isValidDomain(domain) -> null
                else -> Email("$local@$domain")
            }
        }

        private fun isValidDomain(domain: String): Boolean {
            val labels = domain.split('.')
            val top = labels.lastOrNull().orEmpty()
            return domain.length <= MAX_DOMAIN_LENGTH &&
                labels.size >= 2 &&
                top.length >= 2 &&
                top.all { it.isLetter() } &&
                labels.all { isValidLabel(it) }
        }

        private fun isValidLabel(label: String): Boolean =
            label.isNotEmpty() && isShortLabel(label) && isCleanLabel(label)

        private fun isShortLabel(label: String): Boolean = label.length <= MAX_LABEL_LENGTH

        private fun isCleanLabel(label: String): Boolean =
            label.none { it.isWhitespace() } && !label.startsWith("-") && !label.endsWith("-")

        private const val MAX_DOMAIN_LENGTH = 253
        private const val MAX_LABEL_LENGTH = 63
    }
}

/**
 * Номер телефона: только цифры и ведущий «+», длина как в E.164. Пробелы,
 * дефисы и скобки при нормализации исчезают.
 */
class Phone private constructor(
    val value: String,
) {
    override fun equals(other: Any?): Boolean = other is Phone && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        /** Нормализует номер и проверяет его; null, если номер негодный. */
        fun of(raw: String): Phone? {
            val trimmed = raw.trim()
            val digits = trimmed.filter { it.isDigit() }
            val international = trimmed.startsWith("+")
            val allowed = trimmed.all { it.isDigit() || it == '+' || it in FORMATTING }
            return when {
                trimmed.isEmpty() -> null
                !allowed -> null
                digits.length < MIN_DIGITS || digits.length > MAX_DIGITS -> null
                else -> Phone(if (international) "+$digits" else digits)
            }
        }

        private val FORMATTING = charArrayOf(' ', '-', '(', ')', '.', '/')

        private const val MIN_DIGITS = 3
        private const val MAX_DIGITS = 15
    }
}

/**
 * Сайт: схема http или https, host с метками, путь и параметры сохраняются.
 * Схема добавляется сама, host приводится к нижнему регистру и к ASCII.
 */
class Url private constructor(
    val value: String,
) {
    override fun equals(other: Any?): Boolean = other is Url && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        /** Нормализует адрес и проверяет его; null, если адрес негодный. */
        fun of(raw: String): Url? {
            val trimmed = raw.trim()
            val withScheme = if (trimmed.contains("://")) trimmed else "$HTTPS_PREFIX$trimmed"
            val uri = runCatching { URI(withScheme) }.getOrNull()
            val scheme = uri?.scheme?.lowercase(Locale.ROOT)
            val authority = uri?.rawAuthority
            val host = authority?.substringAfterLast('@')?.substringBefore(':')?.lowercase(Locale.ROOT)
            val asciiHost = host?.let { toAsciiHost(it) }
            return when {
                uri == null || scheme == null || asciiHost == null -> null
                scheme == HTTP_SCHEME || scheme == HTTPS_SCHEME -> Url(buildUrl(scheme, asciiHost, uri))
                else -> null
            }
        }

        /** Хост в ASCII: метки без пробелов, дефисов по краям и пустых меток. */
        private fun toAsciiHost(host: String): String? {
            val ascii = runCatching { IDN.toASCII(host) }.getOrNull()
            val labels = ascii?.split('.') ?: return null
            val wellFormed =
                ascii.length <= MAX_HOST_LENGTH &&
                    labels.all { isValidLabel(it) } &&
                    (labels.size >= 2 || ascii == LOCALHOST)
            return if (wellFormed) ascii else null
        }

        private fun isValidLabel(label: String): Boolean {
            val short = label.length <= MAX_LABEL_LENGTH
            val clean = label.none { it.isWhitespace() }
            val edged = !label.startsWith("-") && !label.endsWith("-")
            return label.isNotEmpty() && short && clean && edged
        }

        private fun buildUrl(
            scheme: String,
            host: String,
            uri: URI,
        ): String {
            val port = uri.port.takeIf { it > 0 }?.let { ":$it" } ?: ""
            val path = uri.rawPath.orEmpty()
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val fragment = uri.rawFragment?.let { "#$it" } ?: ""
            return "$scheme://$host$port$path$query$fragment"
        }

        private const val HTTP_SCHEME = "http"
        private const val HTTPS_SCHEME = "https"
        private const val HTTPS_PREFIX = "https://"
        private const val LOCALHOST = "localhost"
        private const val MAX_HOST_LENGTH = 253
        private const val MAX_LABEL_LENGTH = 63
    }
}
