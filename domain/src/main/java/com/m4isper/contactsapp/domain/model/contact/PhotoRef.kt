package com.m4isper.contactsapp.domain.model.contact

/**
 * Ссылка на фото контакта. Провайдер отдаёт её как URI, но домен знает только,
 * что это ссылка на изображение: как её прочитать, решает слой данных.
 */
@JvmInline
value class PhotoRef private constructor(
    val uri: String,
) {
    companion object {
        /** Ссылка из значения провайдера; пустое значение - не ссылка. */
        fun of(uri: String?): PhotoRef? = uri?.takeIf { it.isNotBlank() }?.let { PhotoRef(it) }
    }
}
