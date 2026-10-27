package com.m4isper.contactsapp.domain.model.contact

/**
 * Ссылка на строку контакта в провайдере. Домен не знает, что это за номер
 * и как он устроен в провайдере, - значение создаёт и читает только слой данных.
 */
@JvmInline
value class RawContactRef(
    val value: Long,
)
