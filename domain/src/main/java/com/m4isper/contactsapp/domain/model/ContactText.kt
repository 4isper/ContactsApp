package com.m4isper.contactsapp.domain.model

import java.text.Normalizer
import java.util.Locale

private val WHITESPACE = Regex("\\s+")

/**
 * Нормализует текст для сравнения: Unicode NFKC, обрезка и регистронезависимый
 * lower-case с явной локалью. Не зависит от языка устройства.
 */
fun normalizeContactText(value: String): String =
    Normalizer
        .normalize(value.trim(), Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)

/** Нормализация имён и адресов: дополнительно схлопывает повторные пробелы. */
fun normalizeContactTextForMatch(value: String): String = normalizeContactText(value).replace(WHITESPACE, " ")

/** Цифры телефона без форматирования. */
fun normalizePhoneDigits(value: String): String = value.filter(Char::isDigit)
