package com.m4isper.contactsapp.domain.model

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
    /** Последний аккаунт, выбранный при создании контакта. */
    val lastContactAccount: ContactAccount = ContactAccount.Local,
)
