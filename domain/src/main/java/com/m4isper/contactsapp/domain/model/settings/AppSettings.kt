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
    /** Скрывать содержимое окна в списке недавних задач. */
    val secureWindow: Boolean = false,
    /** Напоминать о днях рождения и событиях. */
    val remindersEnabled: Boolean = false,
    /** За сколько дней предупреждать о дне рождения. */
    val reminderLeadDays: Int = DEFAULT_REMINDER_LEAD_DAYS,
) {
    companion object {
        /** Напоминание за неделю до праздника. */
        const val DEFAULT_REMINDER_LEAD_DAYS = 7
    }
}
