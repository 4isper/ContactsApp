package com.m4isper.contactsapp.domain.vcard

/**
 * Порт vCard-обмена: разбор .vcf-текста в формы и сборка текста из контактов.
 * Формат знает только реализация в слое данных; presentation работает через порт.
 * Методы suspend: файлы бывают большими, работа не должна идти на вызывающем потоке.
 */
interface VCardCodec {
    /**
     * Разбирает .vcf-текст. Пустой результат — в тексте не нашлось ни одной карточки;
     * неразобранные блоки возвращаются в [VCardParseResult.invalid], чтобы импорт
     * мог показать пользователю, что именно пропущено.
     */
    suspend fun parse(text: String): VCardParseResult

    /** Собирает один .vcf-текст со всеми переданными контактами. */
    suspend fun encode(contacts: List<VCardExportContact>): String
}
