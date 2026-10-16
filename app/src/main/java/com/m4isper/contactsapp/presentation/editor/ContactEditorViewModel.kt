package com.m4isper.contactsapp.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.SaveResult
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
import com.m4isper.contactsapp.presentation.contacts.SUBSCRIBE_TIMEOUT_MILLIS
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class EditorPhoneRow(
    val rowId: Long,
    val dataId: Long? = null,
    val number: String = "",
    val kind: PhoneKind = PhoneKind.MOBILE,
    val customLabel: String = "",
)

data class EditorUiState(
    val name: String = "",
    val phones: List<EditorPhoneRow> = emptyList(),
    val isNew: Boolean = true,
    val isBusy: Boolean = false,
    val formError: FormError? = null,
    val errorMessage: String? = null,
    val closeScreen: Boolean = false,
) {
    fun toForm(): ContactForm =
        ContactForm(
            name = name,
            phones =
                phones.map { row ->
                    EditablePhone(
                        dataId = row.dataId,
                        number = row.number,
                        kind = row.kind,
                        customLabel = row.customLabel.takeIf { it.isNotBlank() },
                    )
                },
        )
}

private val rowIds = AtomicLong(0)

private data class EditorFields(
    val name: String,
    val phones: List<EditorPhoneRow>,
    val contactId: Long?,
    val isBusy: Boolean,
)

private data class EditorStatus(
    val formError: FormError?,
    val errorMessage: String?,
    val closeScreen: Boolean,
)

private fun newRow(
    dataId: Long? = null,
    number: String = "",
    kind: PhoneKind = PhoneKind.MOBILE,
    customLabel: String = "",
) = EditorPhoneRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    number = number,
    kind = kind,
    customLabel = customLabel,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactEditorViewModel(
    private val getContactFormUseCase: GetContactFormUseCase,
    private val createContactUseCase: CreateContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
) : ViewModel() {
    private val contactId = MutableStateFlow<Long?>(null)
    private val name = MutableStateFlow("")
    private val phones = MutableStateFlow(listOf(newRow()))
    private val initial = MutableStateFlow(ContactForm(name = ""))
    private val isBusy = MutableStateFlow(false)
    private val formError = MutableStateFlow<FormError?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val closeScreen = MutableStateFlow(false)

    val uiState: StateFlow<EditorUiState> =
        combine(
            combine(name, phones, contactId, isBusy, ::EditorFields),
            combine(formError, errorMessage, closeScreen, ::EditorStatus),
        ) { fields, status ->
            EditorUiState(
                name = fields.name,
                phones = fields.phones,
                isNew = fields.contactId == null,
                isBusy = fields.isBusy,
                formError = status.formError,
                errorMessage = status.errorMessage,
                closeScreen = status.closeScreen,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = EditorUiState(),
        )

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun load(id: Long?) {
        contactId.value = id
        closeScreen.value = false
        formError.value = null
        errorMessage.value = null
        if (id == null) {
            val blank = ContactForm(name = "")
            name.value = ""
            phones.value = listOf(newRow())
            initial.value = blank
        } else {
            viewModelScope.launch {
                isBusy.value = true
                try {
                    val form = getContactFormUseCase(id)
                    if (form == null) {
                        errorMessage.value = null
                        closeScreen.value = true
                    } else {
                        name.value = form.name
                        phones.value =
                            form.phones
                                .map {
                                    newRow(
                                        dataId = it.dataId,
                                        number = it.number,
                                        kind = it.kind,
                                        customLabel = it.customLabel.orEmpty(),
                                    )
                                }.ifEmpty { listOf(newRow()) }
                        initial.value = form
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorMessage.value = e.message
                } finally {
                    isBusy.value = false
                }
            }
        }
    }

    fun onNameChange(value: String) {
        name.value = value
        formError.value = null
    }

    fun onPhoneChange(
        rowId: Long,
        value: String,
    ) {
        phones.value =
            phones.value.map { row ->
                if (row.rowId == rowId) row.copy(number = value) else row
            }
        formError.value = null
    }

    fun onKindChange(
        rowId: Long,
        kind: PhoneKind,
    ) {
        phones.value =
            phones.value.map { row ->
                if (row.rowId == rowId) row.copy(kind = kind) else row
            }
    }

    fun onCustomLabelChange(
        rowId: Long,
        value: String,
    ) {
        phones.value =
            phones.value.map { row ->
                if (row.rowId == rowId) row.copy(customLabel = value) else row
            }
    }

    fun onAddPhone() {
        phones.value = phones.value + newRow()
    }

    fun onRemovePhone(rowId: Long) {
        val updated = phones.value.filterNot { it.rowId == rowId }
        phones.value = updated.ifEmpty { listOf(newRow()) }
        formError.value = null
    }

    fun isDirty(): Boolean =
        ContactForm(name = name.value, phones = phones.value.map { it.toEditable() }) !=
            initial.value

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun onSave() {
        val id = contactId.value
        // Пустые строки — это плейсхолдеры для ввода, а не данные: отбрасываем до валидации.
        val form =
            ContactForm(
                name = name.value,
                phones =
                    phones.value
                        .filter { it.number.isNotBlank() }
                        .map {
                            EditablePhone(
                                dataId = it.dataId,
                                number = it.number,
                                kind = it.kind,
                                customLabel = it.customLabel.takeIf { label -> label.isNotBlank() },
                            )
                        },
            )
        viewModelScope.launch {
            isBusy.value = true
            errorMessage.value = null
            try {
                val result =
                    if (id == null) {
                        createContactUseCase(form)
                    } else {
                        updateContactUseCase(id, form)
                    }
                when (result) {
                    is SaveResult.Saved -> closeScreen.value = true
                    is SaveResult.Invalid -> formError.value = result.error
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isBusy.value = false
            }
        }
    }

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun onDelete() {
        val id = contactId.value ?: return
        viewModelScope.launch {
            isBusy.value = true
            try {
                deleteContactUseCase(id)
                closeScreen.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isBusy.value = false
            }
        }
    }

    private fun EditorPhoneRow.toEditable() =
        EditablePhone(
            dataId = dataId,
            number = number,
            kind = kind,
            customLabel = customLabel.takeIf { it.isNotBlank() },
        )
}
