package com.m4isper.contactsapp.domain.model

import java.io.IOException

/**
 * Ошибка приложения, которую можно показать пользователю. Технические детали
 * (текст исключения провайдера) в интерфейс не попадают: им соответствует вид
 * ошибки, а словарь текстов один на все экраны.
 */
enum class AppError {
    /** Нет разрешения на контакты. */
    PERMISSION_DENIED,

    /** Провайдер контактов недоступен. */
    PROVIDER_FAILED,

    /** Не удалось сохранить изменения. */
    SAVE_FAILED,

    /** Не удалось удалить контакты. */
    DELETE_FAILED,

    /** Не удалось разобрать файл импорта. */
    IMPORT_FAILED,

    /** Не удалось запланировать напоминание. */
    SCHEDULE_FAILED,

    /** Ошибка без подробностей. */
    UNKNOWN,
    ;

    companion object {
        /** Классифицирует исключение: пользователю показывают вид ошибки, а не текст провайдера. */
        fun from(error: Throwable): AppError =
            when (error) {
                is SecurityException -> PERMISSION_DENIED
                is IOException -> PROVIDER_FAILED
                else -> UNKNOWN
            }
    }
}
