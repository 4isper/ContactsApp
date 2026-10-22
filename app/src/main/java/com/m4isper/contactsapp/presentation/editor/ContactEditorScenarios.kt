package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.SaveResult
import com.m4isper.contactsapp.domain.usecase.SetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
import com.m4isper.contactsapp.presentation.common.logw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val EDITOR_TAG = "ContactEditor"

/** Похожие контакты для предупреждения о дубле: сбой не мешает сохранению. */
@Suppress("TooGenericExceptionCaught") // Осознанно: сбой поиска дублей не мешает сохранению.
internal suspend fun loadContacts(getContacts: GetContactsUseCase): List<ContactDetails> =
    try {
        getContacts()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logw(EDITOR_TAG, "load contacts for duplicates failed", e)
        emptyList()
    }

/** Список групп для пикера: сбой чтения не должен ломать редактирование контакта. */
@Suppress("TooGenericExceptionCaught") // Осознанно: сбой списка групп не мешает форме.
internal suspend fun loadGroups(getGroups: GetGroupsUseCase): List<ContactGroup> =
    try {
        getGroups()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logw(EDITOR_TAG, "load groups failed", e)
        emptyList()
    }

/** Аккаунты для нового контакта: сбой чтения оставляет только локальный. */
@Suppress("TooGenericExceptionCaught") // Осознанно: без списка аккаунтов форма остаётся локальной.
internal suspend fun loadAccounts(getAccounts: GetContactAccountsUseCase): List<ContactAccount> {
    val loaded =
        try {
            getAccounts()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(EDITOR_TAG, "load accounts failed", e)
            emptyList()
        }
    return (listOf(ContactAccount.Local) + loaded.filterNot { it.isLocal }).distinct()
}

/** Аккаунт существующего контакта: сбой чтения не мешает редактированию. */
@Suppress("TooGenericExceptionCaught") // Осознанно: аккаунт контакта не мешает форме.
internal suspend fun loadContactAccount(
    getAccount: GetContactAccountUseCase,
    contactId: Long,
): ContactAccount =
    try {
        getAccount(contactId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logw(EDITOR_TAG, "load contact account failed", e)
        ContactAccount.Local
    }

/** Последний выбранный аккаунт: сбой настроек не мешает созданию контакта. */
@Suppress("TooGenericExceptionCaught") // Осознанно: без аккаунта по умолчанию форма остаётся локальной.
internal suspend fun loadDefaultAccount(getDefault: GetDefaultContactAccountUseCase): ContactAccount =
    try {
        getDefault()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logw(EDITOR_TAG, "load default contact account failed", e)
        ContactAccount.Local
    }

/** Последний аккаунт сохраняется для удобства: сбой настройки не мешает форме. */
@Suppress("TooGenericExceptionCaught") // Осознанно: последний аккаунт - удобство, его сбой не важен.
internal suspend fun persistLastAccount(
    setLastAccount: SetLastContactAccountUseCase,
    account: ContactAccount,
) {
    try {
        setLastAccount(account)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logw(EDITOR_TAG, "save last contact account failed", e)
    }
}

/** Сохраняет форму: новый контакт создаётся, существующий - обновляется. */
@Suppress("TooGenericExceptionCaught") // Осознанно: ошибка сохранения показывается в поле формы.
internal suspend fun save(
    state: MutableStateFlow<EditorUiState>,
    createContact: CreateContactUseCase,
    updateContact: UpdateContactUseCase,
    id: Long?,
    form: ContactForm,
) {
    state.update { it.copy(isBusy = true, errorMessage = null, fieldErrors = emptyMap()) }
    try {
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
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logw(EDITOR_TAG, "save contact failed", e)
        state.update { it.copy(errorMessage = e.message) }
    } finally {
        state.update { it.copy(isBusy = false) }
    }
}
