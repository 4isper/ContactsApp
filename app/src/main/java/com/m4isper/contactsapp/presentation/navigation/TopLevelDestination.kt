package com.m4isper.contactsapp.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Merge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.BrandIcons

/** Верхнеуровневые разделы приложения: на широких окнах доступны из рейла. */
internal enum class TopLevelDestination(
    val key: NavKey,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Contacts(ContactsListKey, R.string.app_name, Icons.Filled.Contacts),
    Groups(GroupsKey, R.string.groups_title, Icons.AutoMirrored.Filled.Label),
    Recents(RecentsKey, R.string.recents_title, Icons.Filled.History),
    Duplicates(DuplicatesKey, R.string.duplicates_title, Icons.Filled.Merge),
    Settings(SettingsKey, R.string.settings_menu, BrandIcons.Settings),
}

internal fun topLevelDestinationOf(key: NavKey?): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { it.key == key }

/**
 * Рейл нужен только на средних и широких окнах и только на верхнем уровне:
 * в деталях экран отдаёт ширину двум панелям list-detail.
 */
internal fun showNavigationRail(
    isMediumOrWider: Boolean,
    destination: TopLevelDestination?,
): Boolean = isMediumOrWider && destination != null

/**
 * Переключение раздела: стек сжимается до корня, чтобы переходы внутри
 * раздела не копились, а назад с раздела ведёт на список контактов.
 */
internal fun navigateToTopLevel(
    backStack: MutableList<NavKey>,
    destination: TopLevelDestination,
) {
    while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    if (destination != TopLevelDestination.Contacts) backStack.add(destination.key)
}
