package com.m4isper.contactsapp.domain.model

/** Тема приложения: как в системе или принудительно светлая/тёмная. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Сортировка списка контактов: по полному имени или по фамилии. */
enum class ContactSortOrder {
    BY_NAME,
    BY_FAMILY,
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
)
