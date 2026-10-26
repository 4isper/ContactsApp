package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount

/**
 * Действие экрана списка: один контракт вместо десятков колбэков. Экран
 * разбирает действие и зовёт нужный сценарий, а листья получают свои лямбды.
 */
sealed interface ContactsUiAction

/** Действия состояния списка: фильтры, выделение, избранное и группы. */
sealed interface ContactsListAction : ContactsUiAction {
    data class SearchQueryChanged(
        val query: String,
    ) : ContactsListAction

    data object FavoritesOnlyToggled : ContactsListAction

    data class ContactSelected(
        val contactId: Long,
    ) : ContactsListAction

    data object ClearSelection : ContactsListAction

    data class SelectAll(
        val contactIds: List<Long>,
    ) : ContactsListAction

    data class GroupSelected(
        val groupId: Long?,
    ) : ContactsListAction

    data class AccountSelected(
        val account: ContactAccount?,
    ) : ContactsListAction

    data class CreateGroup(
        val title: String,
    ) : ContactsListAction

    data class AddSelectedToGroup(
        val groupIds: Set<Long>,
    ) : ContactsListAction

    data class ToggleFavorite(
        val contactId: Long,
    ) : ContactsListAction

    data object ToggleFavoriteSelected : ContactsListAction
}

/** Действия файлов и удаления контактов. */
sealed interface ContactsFileAction : ContactsUiAction {
    data object Retry : ContactsFileAction

    data object Refresh : ContactsFileAction

    /** Повторная синхронизация из индикатора аккаунта: обновляет список и сообщает итог. */
    data object RetrySync : ContactsFileAction

    data object DeleteSelected : ContactsFileAction

    data object UndoDelete : ContactsFileAction

    data object DeleteWindowClosed : ContactsFileAction

    data object ShareSelected : ContactsFileAction

    data class ImportText(
        val text: String,
    ) : ContactsFileAction

    data object ExportRequested : ContactsFileAction

    data object ExportSelectionRequested : ContactsFileAction

    data object EventHandled : ContactsFileAction
}

/** Переходы к другим экранам. */
sealed interface ContactsNavigationAction : ContactsUiAction {
    data class ContactClicked(
        val contactId: Long,
    ) : ContactsNavigationAction

    data object AddClicked : ContactsNavigationAction

    data object ManageGroupsClicked : ContactsNavigationAction

    data object DuplicatesClicked : ContactsNavigationAction

    data object RecentsClicked : ContactsNavigationAction

    data object SettingsClicked : ContactsNavigationAction

    data class ProfileClicked(
        val contactId: Long,
    ) : ContactsNavigationAction
}
