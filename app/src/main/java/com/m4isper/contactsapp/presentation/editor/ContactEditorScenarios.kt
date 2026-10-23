package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.usecase.contacts.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.SaveResult
import com.m4isper.contactsapp.domain.usecase.contacts.SetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.UpdateContactUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.presentation.common.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val EDITOR_TAG = "ContactEditor"

/** Похожие контакты для предупреждения о дубле: сбой не мешает сохранению. */
internal suspend fun loadContacts(getContacts: GetContactsUseCase): List<ContactDetails> =
    safeCall(EDITOR_TAG, "load contacts for duplicates failed") {
        getContacts()
    }.getOrElse { emptyList() }

/** Список групп для пикера: сбой чтения не должен ломать редактирование контакта. */
internal suspend fun loadGroups(getGroups: GetGroupsUseCase): List<ContactGroup> =
    safeCall(EDITOR_TAG, "load groups failed") {
        getGroups()
    }.getOrElse { emptyList() }

/** Аккаунты для нового контакта: сбой чтения оставляет только локальный. */
internal suspend fun loadAccounts(getAccounts: GetContactAccountsUseCase): List<ContactAccount> {
    val loaded =
        safeCall(EDITOR_TAG, "load accounts failed") {
            getAccounts()
        }.getOrElse { emptyList() }

    return (listOf(ContactAccount.Local) + loaded.filterNot { it.isLocal }).distinct()
}

/** Аккаунт существующего контакта: сбой чтения не мешает редактированию. */
internal suspend fun loadContactAccount(
    getAccount: GetContactAccountUseCase,
    contactId: Long,
): ContactAccount =
    safeCall(EDITOR_TAG, "load contact account failed") {
        getAccount(contactId)
    }.getOrElse { ContactAccount.Local }

/** Последний выбранный аккаунт: сбой настроек не мешает созданию контакта. */
internal suspend fun loadDefaultAccount(getDefault: GetDefaultContactAccountUseCase): ContactAccount =
    safeCall(EDITOR_TAG, "load default contact account failed") {
        getDefault()
    }.getOrElse { ContactAccount.Local }

/** Последний аккаунт сохраняется для удобства: сбой настройки не мешает форме. */
internal suspend fun persistLastAccount(
    setLastAccount: SetLastContactAccountUseCase,
    account: ContactAccount,
) {
    safeCall(EDITOR_TAG, "save last contact account failed") {
        setLastAccount(account)
    }
}

/** Сохраняет форму: новый контакт создаётся, существующий - обновляется. */
internal suspend fun save(
    state: MutableStateFlow<EditorUiState>,
    createContact: CreateContactUseCase,
    updateContact: UpdateContactUseCase,
    id: Long?,
    form: ContactForm,
) {
    try {
        state.update { it.copy(isBusy = true, error = null, fieldErrors = emptyMap()) }
        safeCall(EDITOR_TAG, "save contact failed") {
            val result =
                if (id == null) {
                    createContact(form, state.value.selectedAccount ?: ContactAccount.Local)
                } else {
                    updateContact(id, form)
                }
            when (result) {
                is SaveResult.Saved -> state.update { it.copy(closeScreen = true) }
                is SaveResult.Invalid -> state.update { it.copy(formError = result.error) }
            }
        }.onFailure { failure ->
            state.update { it.copy(error = AppError.SAVE_FAILED) }
        }
    } finally {
        state.update { it.copy(isBusy = false) }
    }
}
