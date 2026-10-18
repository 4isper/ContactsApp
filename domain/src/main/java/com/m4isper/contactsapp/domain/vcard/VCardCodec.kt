package com.m4isper.contactsapp.domain.vcard

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm

/**
 * Порт vCard-обмена: разбор .vcf-текста в формы и сборка текста из контактов.
 * Формат знает только реализация в слое данных; presentation работает через порт.
 */
interface VCardCodec {
    /** Разбирает .vcf-текст; пустой список — в тексте не нашлось ни одной карточки. */
    fun parse(text: String): List<ContactForm>

    /** Собирает один .vcf-текст со всеми переданными контактами. */
    fun encode(contacts: List<ContactDetails>): String
}
