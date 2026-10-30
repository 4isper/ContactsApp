package com.m4isper.contactsapp.presentation.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.FormError
import com.m4isper.contactsapp.domain.model.contact.FormField
import com.m4isper.contactsapp.presentation.common.groups.GroupPickerSheet
import com.m4isper.contactsapp.presentation.common.message
import com.m4isper.contactsapp.presentation.editor.components.AccountPickerDialog
import com.m4isper.contactsapp.presentation.editor.components.AccountRow
import com.m4isper.contactsapp.presentation.editor.components.SectionHeader
import com.m4isper.contactsapp.presentation.editor.dialogs.BirthdayPickerDialog
import com.m4isper.contactsapp.presentation.editor.dialogs.CropPhotoDialog
import com.m4isper.contactsapp.presentation.editor.edits.withBirthday
import com.m4isper.contactsapp.presentation.editor.edits.withEventDate
import com.m4isper.contactsapp.presentation.editor.edits.withPhoto
import com.m4isper.contactsapp.presentation.editor.edits.withToggledGroup
import com.m4isper.contactsapp.presentation.editor.edits.withoutPhoto
import com.m4isper.contactsapp.presentation.editor.items.editorContactItems
import com.m4isper.contactsapp.presentation.editor.items.editorExtraItems
import com.m4isper.contactsapp.presentation.editor.items.errorItem
import com.m4isper.contactsapp.presentation.editor.items.groupsItem
import com.m4isper.contactsapp.presentation.editor.items.nameItem
import com.m4isper.contactsapp.presentation.editor.items.nicknameItem
import com.m4isper.contactsapp.presentation.editor.rows.PhotoEditor
import com.m4isper.contactsapp.ui.adaptive.WindowWidthClass
import com.m4isper.contactsapp.ui.adaptive.rememberWindowWidthClass
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun EditorForm(
    viewModel: ContactEditorViewModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val edit: ((EditorUiState) -> EditorUiState) -> Unit = viewModel::update
    var showDatePicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var eventPickerRowId by remember { mutableStateOf<Long?>(null) }
    // Режим открытия пикера: из кнопки даты режим берёт само значение,
    // из switch «Без года» — всегда с годом, чтобы год выбрали явно.
    var birthdayPickerNoYear by remember { mutableStateOf(false) }
    var eventPickerNoYear by remember { mutableStateOf(false) }
    var cropSource by remember { mutableStateOf<Uri?>(null) }

    cropSource?.let { source ->
        CropPhotoDialog(
            source = source,
            onConfirm = { cropped ->
                cropSource = null
                edit { it.withPhoto(cropped.toString()) }
            },
            onDismiss = { cropSource = null },
        )
    }

    if (uiState.similarContacts.isNotEmpty()) {
        SimilarContactsDialog(
            contacts = uiState.similarContacts,
            onConfirm = viewModel::onSaveConfirmed,
            onDismiss = viewModel::onSaveDismissed,
        )
    }

    val listState = rememberLazyListState()
    val rightListState = rememberLazyListState()
    val itemIndices = remember { FormItemIndices() }
    val rightItemIndices = remember { FormItemIndices() }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    fun focusFor(key: String): FocusRequester = focusRequesters.getOrPut(key) { FocusRequester() }

    // Блокированное сохранение: показываем баннер и подводим пользователя к первой ошибке.
    // На широком окне строка с ошибкой может быть в любой из двух колонок.
    LaunchedEffect(uiState.firstErrorRowIds) {
        val scrolled =
            scrollToFirstError(uiState.firstErrorRowIds, itemIndices, listState, focusRequesters)
        if (!scrolled) {
            scrollToFirstError(uiState.firstErrorRowIds, rightItemIndices, rightListState, focusRequesters)
        }
    }

    val twoColumns = rememberWindowWidthClass() == WindowWidthClass.EXPANDED

    // Слева — кто это и как связаться, справа — работа, даты, связи и заметка.
    val leftItems: LazyListScope.() -> Unit = {
        identityItems(
            uiState = uiState,
            edit = edit,
            itemIndices = itemIndices,
            onAccountClick = { showAccountPicker = true },
            onGroupsClick = { showGroupPicker = true },
            onPhotoCrop = { cropSource = it },
            onPhotoRemove = { edit { it.withoutPhoto() } },
        )
        editorContactItems(uiState = uiState, edit = edit, itemIndices = itemIndices, focusFor = ::focusFor)
    }
    val rightItems: LazyListScope.(FormItemIndices) -> Unit = { indices ->
        editorExtraItems(
            uiState = uiState,
            edit = edit,
            itemIndices = indices,
            focusFor = ::focusFor,
            onBirthdayClick = {
                birthdayPickerNoYear = uiState.birthday.value?.year == null
                showDatePicker = true
            },
            onEventDateClick = { rowId ->
                eventPickerNoYear =
                    uiState.events
                        .firstOrNull { it.rowId == rowId }
                        ?.value
                        ?.date
                        ?.year == null
                eventPickerRowId = rowId
            },
            onBirthdayAddYear = {
                birthdayPickerNoYear = false
                showDatePicker = true
            },
            onEventAddYear = { rowId ->
                eventPickerNoYear = false
                eventPickerRowId = rowId
            },
        )
        deleteItem(uiState, indices, onDeleteClick)
    }

    EditorBody(
        twoColumns = twoColumns,
        modifier = modifier,
        listState = listState,
        rightListState = rightListState,
        left = {
            itemIndices.reset()
            leftItems()
        },
        right = {
            if (twoColumns) {
                rightItemIndices.reset()
                rightItems(rightItemIndices)
            } else {
                // На узком окне вторая половина формы — продолжение того же списка.
                rightItems(itemIndices)
            }
        },
    )

    if (showDatePicker) {
        BirthdayPickerDialog(
            birthday = uiState.birthday.value,
            startNoYear = birthdayPickerNoYear,
            onConfirmed = { birthday ->
                edit { it.withBirthday(birthday) }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    val eventPickerRow = uiState.events.firstOrNull { it.rowId == eventPickerRowId }
    if (eventPickerRow != null) {
        BirthdayPickerDialog(
            birthday = eventPickerRow.value.date,
            startNoYear = eventPickerNoYear,
            confirmTestTag = "editor_event_date_confirm",
            onConfirmed = { birthday ->
                edit { it.withEventDate(eventPickerRow.rowId, birthday) }
                eventPickerRowId = null
            },
            onDismiss = { eventPickerRowId = null },
        )
    }

    if (showGroupPicker) {
        GroupPickerSheet(
            title = stringResource(R.string.editor_section_groups),
            groups = uiState.availableGroups,
            selectedIds = uiState.selectedGroupIds,
            confirmLabel = stringResource(R.string.action_done),
            onToggle = { groupId -> edit { it.withToggledGroup(groupId) } },
            onCreateGroup = viewModel::onCreateGroup,
            onConfirm = { showGroupPicker = false },
            onDismiss = { showGroupPicker = false },
        )
    }

    if (showAccountPicker) {
        AccountPickerDialog(
            accounts = uiState.accounts,
            selected = uiState.selectedAccount,
            onSelect = { account ->
                viewModel.onAccountSelected(account)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }
}

/**
 * Контейнер формы: один список на узком окне, две колонки — на широком.
 * Порядок полей сохраняется: колонки читаются сверху вниз, левая, затем правая.
 */
@Composable
internal fun EditorBody(
    twoColumns: Boolean,
    left: LazyListScope.() -> Unit,
    right: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    rightListState: LazyListState = rememberLazyListState(),
) {
    if (!twoColumns) {
        LazyColumn(
            state = listState,
            modifier =
                modifier
                    .fillMaxSize()
                    .imePadding()
                    .testTag("editor_form"),
        ) {
            left()
            right()
        }
        return
    }
    Row(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .testTag("editor_form"),
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("editor_form_left"),
        ) {
            left()
        }
        LazyColumn(
            state = rightListState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("editor_form_right"),
        ) {
            right()
        }
    }
}

/**
 * Фото, аккаунт, ошибки, имя, ник и группы: начало формы.
 * На широком окне эта часть — левая колонка, на узком — верх списка.
 */
private fun LazyListScope.identityItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
    itemIndices: FormItemIndices,
    onAccountClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onPhotoCrop: (Uri) -> Unit,
    onPhotoRemove: () -> Unit,
) {
    itemIndices.register("photo")
    item(key = "photo") {
        EditorPhotoSection(
            name = uiState.effectiveName,
            photoUri = uiState.photoUri,
            context = LocalContext.current,
            onCrop = onPhotoCrop,
            onRemove = onPhotoRemove,
        )
    }
    // Аккаунт подсвечивается только собственной ошибкой выбора, а не любой сетевой/полевой.
    accountItems(
        uiState = uiState,
        onClick = onAccountClick,
        isError = uiState.error == AppError.LOCAL_ACCOUNT_BLOCKED,
        itemIndices = itemIndices,
    )
    errorItem(uiState, itemIndices)
    nameItem(uiState, edit, itemIndices)
    nicknameItem(uiState, edit, itemIndices)
    groupsItem(uiState, itemIndices, onGroupsClick)
}

/** Удаление контакта: последняя строка формы в любой раскладке. */
private fun LazyListScope.deleteItem(
    uiState: EditorUiState,
    itemIndices: FormItemIndices,
    onDeleteClick: () -> Unit,
) {
    if (uiState.isNew) return
    itemIndices.register("delete")
    item(key = "delete") {
        val spacing = LocalSpacing.current
        OutlinedButton(
            onClick = onDeleteClick,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(spacing.m)
                    .testTag("editor_delete_button"),
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(spacing.s))
            Text(stringResource(R.string.editor_delete))
        }
    }
}

/**
 * Новому контакту — выбор аккаунта, если есть из чего выбирать;
 * существующему — аккаунт только для чтения.
 */
private fun LazyListScope.accountItems(
    uiState: EditorUiState,
    onClick: () -> Unit,
    isError: Boolean = false,
    itemIndices: FormItemIndices,
) {
    if (uiState.isNew) {
        if (uiState.accounts.size <= 1) return
        itemIndices.register("account-header")
        itemIndices.register("account")
        item(key = "account-header") {
            SectionHeader(titleRes = R.string.editor_section_account)
        }
        item(key = "account") {
            AccountRow(account = uiState.selectedAccount, onClick = onClick, isError = isError)
        }
    } else {
        val account = uiState.selectedAccount ?: return
        itemIndices.register("account-header")
        itemIndices.register("account")
        item(key = "account-header") {
            SectionHeader(titleRes = R.string.editor_section_account)
        }
        item(key = "account") {
            AccountRow(account = account, onClick = null, isError = isError)
        }
    }
}

/**
 * Фото контакта: галерея, камера и обрезка. Снимок камеры и выбранный файл
 * попадают в один редактор обрезки.
 */
@Composable
private fun EditorPhotoSection(
    name: String,
    photoUri: String?,
    context: android.content.Context,
    onCrop: (Uri) -> Unit,
    onRemove: () -> Unit,
) {
    val photoPicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onCrop(it) }
        }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { saved ->
            if (saved) cameraUri?.let { onCrop(it) }
        }
    PhotoEditor(
        name = name,
        photoUri = photoUri,
        onPick = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onTakePhoto = {
            val uri = cameraOutputUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        },
        onRemove = onRemove,
    )
}

/**
 * Предупреждение о дубле: похожие контакты и выбор — сохранить всё равно или отменить.
 */
@Composable
private fun SimilarContactsDialog(
    contacts: List<ContactDetails>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_duplicate_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.editor_duplicate_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                // Ограниченная высота: длинный список похожих контактов скроллится,
                // а кнопки диалога остаются на экране.
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                ) {
                    items(contacts, key = { it.contactId }) { contact ->
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = spacing.s),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("editor_duplicate_confirm"),
            ) {
                Text(stringResource(R.string.editor_duplicate_save_anyway))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag("editor_duplicate_dialog"),
    )
}

/** Файл для снимка камеры: провайдеру отдаём URI через FileProvider. */
private fun cameraOutputUri(context: android.content.Context): Uri {
    val file =
        java.io.File(
            com.m4isper.contactsapp.presentation.common.share
                .sharedCacheDir(context),
            "camera_${System.currentTimeMillis()}.jpg",
        )
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

/** Текст ошибки поля: показываем проблему рядом с полем. */
@Composable
internal fun FormError.fieldMessage(): String =
    stringResource(
        when (this) {
            FormError.EMPTY_FORM -> R.string.form_error_empty
            FormError.INVALID_PHONE -> R.string.form_error_phone
            FormError.INVALID_EMAIL -> R.string.form_error_email
            FormError.INVALID_URL -> R.string.form_error_url
            FormError.INVALID_DATE -> R.string.form_error_date
        },
    )

/** Текст ошибки формы: удаление, валидация или сообщение исключения. */
@Composable
internal fun formMessage(uiState: EditorUiState): String =
    when {
        uiState.deleteFailed -> stringResource(R.string.batch_delete_failed)
        uiState.error != null -> uiState.error.message()
        // У каждой ветки FormError есть сообщение: пустая строка ошибки невозможна.
        uiState.formError != null -> uiState.formError.fieldMessage()
        uiState.fieldErrors.isNotEmpty() -> stringResource(R.string.editor_check_fields)
        else -> ""
    }

/**
 * Индексы элементов формы в LazyColumn: считаются в порядке регистрации блоков
 * и позволяют прокрутить список к строке с ошибкой.
 */
internal class FormItemIndices {
    private val indices = mutableMapOf<String, Int>()
    private var next = 0

    /** Сброс перед новым проходом построения содержимого списка. */
    fun reset() {
        indices.clear()
        next = 0
    }

    fun register(key: String) {
        indices[key] = next
        next++
    }

    /** Регистрирует блок из [count] элементов без индивидуальных ключей. */
    fun registerCount(count: Int) {
        next += count
    }

    operator fun get(key: String): Int? = indices[key]
}

/**
 * Подводит список к первой строке с ошибкой и ставит фокус в её поле.
 * Возвращает false, если строка живёт в другой колонке: тогда её ищет вызывающий.
 */
private suspend fun scrollToFirstError(
    targets: Map<FormField, Long>,
    itemIndices: FormItemIndices,
    listState: LazyListState,
    focusRequesters: MutableMap<String, FocusRequester>,
): Boolean {
    val target = targets.entries.firstOrNull()
    val key = target?.let { "row_${it.value}" }
    val index = key?.let(itemIndices::get)
    if (key == null || index == null) return false
    listState.animateScrollToItem(index)
    runCatching { focusRequesters[key]?.requestFocus() }
    return true
}
