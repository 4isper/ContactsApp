package com.m4isper.contactsapp.domain.model

/**
 * Связь контактов: целевая строка контакта и присоединённые к ней источники.
 * Хранится, чтобы слияние можно было отменить.
 */
data class ContactLink(
    val target: RawContactRef,
    val sources: List<RawContactRef>,
)
