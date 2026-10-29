package com.m4isper.contactsapp.domain.model.settings

import com.m4isper.contactsapp.domain.model.contact.ContactAccount

/** Тема приложения: как в системе или принудительно светлая/тёмная. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Сортировка списка контактов: имя, фамилия, компания или недавние изменения. */
enum class ContactSortOrder {
    BY_NAME,
    BY_NAME_DESC,
    BY_FAMILY,
    BY_COMPANY,
    RECENTLY_ADDED,
}

/** Плотность списка: обычные карточки или компактные. */
enum class ListDensity {
    COMFORTABLE,
    COMPACT,
}

/** Пользовательские настройки приложения; по умолчанию — системная тема и обычный список. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sortOrder: ContactSortOrder = ContactSortOrder.BY_NAME,
    val listDensity: ListDensity = ListDensity.COMFORTABLE,
    /** Показывать избранные контакты отдельной закреплённой секцией сверху списка. */
    val favoritesFirst: Boolean = true,
    /** Последний аккаунт, выбранный при создании контакта. */
    val lastContactAccount: ContactAccount = ContactAccount.Local,
    /** Аккаунт, в который попадают новые контакты; по умолчанию — локальный. */
    val defaultContactAccount: ContactAccount = ContactAccount.Local,
    /** Язык приложения: пусто — язык системы. */
    val appLanguage: String = "",
    /** Динамические цвета Android 12+ вместо фирменной палитры. */
    val dynamicColor: Boolean = true,
    /** Показывать фотографии контактов. */
    val showPhotos: Boolean = true,
    /** Защита приложения PIN-кодом. */
    val appLockEnabled: Boolean = false,
    /** Хэш PIN-кода защиты; пусто — код не задан. */
    val appLockPinHash: String = "",
    /** Длина заданного PIN-кода; 0 — неизвестна (установки до обновления). */
    val appLockPinLength: Int = 0,
    /** Скрывать содержимое окна в списке недавних задач. */
    val secureWindow: Boolean = false,
    /** Разблокировка по биометрии, когда защита включена; PIN остаётся запасным путём. */
    val appLockBiometrics: Boolean = true,
    /** Напоминать о днях рождения и событиях. */
    val remindersEnabled: Boolean = false,
    /** За сколько дней предупреждать о дне рождения. */
    val reminderLeadDays: Int = DEFAULT_REMINDER_LEAD_DAYS,
    /** Время суток ежедневного напоминания. */
    val reminderTime: ReminderTime = DEFAULT_REMINDER_TIME,
) {
    companion object {
        /** Напоминание за неделю до праздника. */
        const val DEFAULT_REMINDER_LEAD_DAYS = 7

        /** Напоминание приходит утром, в девять. */
        val DEFAULT_REMINDER_TIME = ReminderTime(hour = 9, minute = 0)
    }
}
