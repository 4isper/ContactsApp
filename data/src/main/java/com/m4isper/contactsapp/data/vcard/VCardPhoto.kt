package com.m4isper.contactsapp.data.vcard

import java.util.Base64

/** Лимит размера фото в .vcf: больше — фото пропускается, контакт импортируется без него. */
internal const val MAX_PHOTO_BYTES = 512 * 1024

/** Ширина строки в .vcf: длинные значения сворачиваются с пробелом-отступом. */
private const val FOLD_WIDTH = 75

/** Число символов base64 на 3 байта данных. */
private const val BASE64_CHUNK = 4

/** Сколько байт данных даёт один блок base64. */
private const val BASE64_BYTES = 3

/** Что удалось вытащить из свойства PHOTO. */
internal enum class PhotoStatus {
    /** Фото в файле нет. */
    NONE,

    /** Фото разобрано. */
    OK,

    /** Фото есть, но оно больше лимита. */
    TOO_LARGE,

    /** Фото в неподдерживаемом виде (ссылка, не base64). */
    UNSUPPORTED,
}

/** Результат разбора свойства PHOTO. */
internal data class PhotoPayload(
    val status: PhotoStatus,
    val bytes: ByteArray? = null,
)

/** Фото в base64: длинная строка сворачивается, как требует формат .vcf. */
internal fun encodePhoto(bytes: ByteArray): String =
    Base64
        .getEncoder()
        .encodeToString(bytes)
        .chunked(FOLD_WIDTH)
        .joinToString(separator = "\n ")

/**
 * Разбор фото из свойства PHOTO: поддерживаются data-URI с base64 и обычный base64
 * (vCard 2.1/3.0/4.0). Ссылки на файлы пропускаем — скачивать их нельзя.
 */
internal fun decodePhoto(raw: String): PhotoPayload {
    val value = raw.trim()
    val payload = photoPayloadOf(value).orEmpty()
    val cleaned = payload.filterNot { it.isWhitespace() }
    val bytes =
        if (cleaned.isEmpty()) {
            null
        } else {
            runCatching { Base64.getMimeDecoder().decode(cleaned) }.getOrNull()
        }
    return when {
        value.isEmpty() -> PhotoPayload(PhotoStatus.NONE)
        payload.isEmpty() || bytes == null -> PhotoPayload(PhotoStatus.UNSUPPORTED)
        cleaned.length / BASE64_CHUNK * BASE64_BYTES > MAX_PHOTO_BYTES -> PhotoPayload(PhotoStatus.TOO_LARGE)
        bytes.size > MAX_PHOTO_BYTES -> PhotoPayload(PhotoStatus.TOO_LARGE)
        else -> PhotoPayload(PhotoStatus.OK, bytes)
    }
}

/** Часть свойства PHOTO с данными фото; null, если формат не поддерживается. */
private fun photoPayloadOf(value: String): String? =
    when {
        value.startsWith(DATA_URI_PREFIX, ignoreCase = true) ->
            value.substringAfter(BASE64_MARKER, "").takeIf { it.isNotEmpty() }
        value.contains("://") -> null
        else -> value
    }

private const val DATA_URI_PREFIX = "data:"

private const val BASE64_MARKER = "base64,"
