package com.m4isper.contactsapp.presentation.intent

import android.content.ContentResolver
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/**
 * Действие, с которого приложение запущено системой: открыть контакт, отредактировать,
 * создать, выбрать контакт для другой программы или принять .vcf.
 */
sealed interface StartAction {
    /** Обычный запуск приложения. */
    data object List : StartAction

    /** Открыть карточку контакта. */
    data class OpenContact(
        val contactId: Long,
    ) : StartAction

    /** Открыть контакт в редакторе. */
    data class EditContact(
        val contactId: Long,
    ) : StartAction

    /** Создать новый контакт. */
    data object NewContact : StartAction

    /** Выбрать контакт для программы, которая запросила выбор. */
    data object PickContact : StartAction

    /** Открыть список избранного: ярлык приложения. */
    data object ShowFavorites : StartAction

    /** Принять .vcf: показать предпросмотр импорта. */
    data class ImportVCard(
        val text: String,
    ) : StartAction
}

/** Разбор системного intent: какое действие приложению нужно выполнить. */
internal fun startActionOf(
    intent: Intent?,
    resolver: ContentResolver,
): StartAction {
    if (intent == null) return StartAction.List
    return when (intent.action) {
        Intent.ACTION_VIEW -> viewActionOf(intent, resolver)
        Intent.ACTION_EDIT -> editActionOf(intent)
        Intent.ACTION_INSERT -> StartAction.NewContact
        Intent.ACTION_PICK -> StartAction.PickContact
        ACTION_SHOW_FAVORITES -> StartAction.ShowFavorites
        ACTION_NEW_CONTACT -> StartAction.NewContact
        Intent.ACTION_SEND -> sendActionOf(intent, resolver)
        else -> StartAction.List
    }
}

/** Просмотр: карточка контакта или файл .vcf. */
private fun viewActionOf(
    intent: Intent,
    resolver: ContentResolver,
): StartAction {
    val uri = intent.data
    return when {
        intent.type in VCARD_MIME_TYPES -> StartAction.ImportVCard(intent.vCardText(resolver))
        uri != null && intent.isContactUri() -> StartAction.OpenContact(uri.contactId())
        else -> StartAction.List
    }
}

/** Редактирование: карточка контакта. */
private fun editActionOf(intent: Intent): StartAction {
    val uri = intent.data ?: return StartAction.List
    return if (intent.isContactUri()) StartAction.EditContact(uri.contactId()) else StartAction.List
}

/** Передача .vcf из другой программы. */
private fun sendActionOf(
    intent: Intent,
    resolver: ContentResolver,
): StartAction =
    if (intent.type in VCARD_MIME_TYPES) {
        StartAction.ImportVCard(intent.vCardText(resolver))
    } else {
        StartAction.List
    }

/**
 * URI контакта: item (один контакт) или dir (папка контактов).
 * Система передаёт контакт как content-URI провайдера: MIME-типа в intent при этом нет,
 * поэтому ориентируемся на адрес и путь.
 */
private fun Intent.isContactUri(): Boolean {
    val uri = data ?: return false
    val type = type
    val hasContactType =
        type != null &&
            (
                type == ContactsContract.Contacts.CONTENT_TYPE ||
                    type == ContactsContract.Contacts.CONTENT_ITEM_TYPE
            )
    val segment = uri.pathSegments.firstOrNull()
    val isProviderUri =
        uri.scheme == ContentResolver.SCHEME_CONTENT &&
            uri.authority == ContactsContract.AUTHORITY &&
            (segment == "contacts" || segment == "lookup")
    return hasContactType || isProviderUri
}

/** id контакта из URI: у lookup-контакта он тоже последний сегмент. */
private fun Uri.contactId(): Long = lastPathSegment?.toLongOrNull() ?: 0L

/** Текст .vcf из intent: либо данные, либо переданный файл, либо EXTRA_STREAM. */
private fun Intent.vCardText(resolver: ContentResolver): String =
    data?.let { uri -> uri.readText(resolver) }
        ?: (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM)
        })?.let { uri -> uri.readText(resolver) }
        ?: getStringExtra(Intent.EXTRA_TEXT)
        ?: ""

private fun Uri.readText(resolver: ContentResolver): String =
    runCatching {
        resolver.openInputStream(this)?.bufferedReader()?.use { it.readText() }
    }.getOrNull().orEmpty()

/** Действие ярлыка «Избранное». */
const val ACTION_SHOW_FAVORITES = "com.m4isper.contactsapp.action.SHOW_FAVORITES"

/** Действие ярлыка «Новый контакт». */
const val ACTION_NEW_CONTACT = "com.m4isper.contactsapp.action.NEW_CONTACT"

private val VCARD_MIME_TYPES =
    setOf(
        "text/vcard",
        "text/x-vcard",
        "text/directory",
        "application/vcard",
    )
