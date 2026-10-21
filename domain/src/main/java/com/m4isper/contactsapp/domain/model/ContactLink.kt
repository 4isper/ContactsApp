package com.m4isper.contactsapp.domain.model

/**
 * Связь контактов: целевой raw-контакт и присоединённые к нему источники.
 * Хранится, чтобы слияние можно было отменить.
 */
data class ContactLink(
    val targetRawContactId: Long,
    val sourceRawContactIds: List<Long>,
)
