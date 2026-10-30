package com.m4isper.contactsapp.presentation.contacts.effects

/** Итог экспорта для баннера: текст остаётся на экране, пока его не закроют. */
internal data class ExportSummary(
    val message: String,
    val failed: Boolean = false,
)
