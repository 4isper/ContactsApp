package com.m4isper.contactsapp.presentation.editor.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Диалог выбора даты с режимом «без года». Синтетический високосный [DEFAULT_PICKER_YEAR]
 * нужен только календарю: в режиме без года он никогда не попадает в модель.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BirthdayPickerDialog(
    birthday: Birthday?,
    startNoYear: Boolean,
    onConfirmed: (Birthday) -> Unit,
    onDismiss: () -> Unit,
    confirmTestTag: String = "editor_birthday_confirm",
) {
    var noYear by rememberSaveable { mutableStateOf(startNoYear) }
    val initialYear = pickerInitialYear(birthday, startNoYear)
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                birthday?.let { value ->
                    LocalDate
                        .of(initialYear, value.month, value.day)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                },
        )
    val spacing = LocalSpacing.current
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerBirthdayResult(
                        selectedDateMillis = pickerState.selectedDateMillis,
                        birthday = birthday,
                        initialYear = initialYear,
                        noYear = noYear,
                    )?.let(onConfirmed)
                },
                modifier = Modifier.testTag(confirmTestTag),
            ) {
                Text(stringResource(R.string.editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        Column {
            // Режим над календарём: он виден до выбора даты и не уезжает за экран.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // Тег на toggleable-строке: клик обрабатывает строка, не Switch.
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("editor_no_year_dialog_switch")
                        .padding(horizontal = spacing.l, vertical = spacing.s)
                        .toggleable(
                            value = noYear,
                            role = Role.Switch,
                            onValueChange = { noYear = it },
                        ),
            ) {
                Text(
                    text = stringResource(R.string.editor_birthday_no_year),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = noYear,
                    onCheckedChange = null,
                )
            }
            DatePicker(state = pickerState, showModeToggle = false, title = null)
        }
    }
}

/**
 * Год для показа в календаре: у значения с годом — сам год; у значения без года —
 * синтетический високосный 2000; без значения — текущий год как подтверждаемое умолчание.
 */
internal fun pickerInitialYear(
    birthday: Birthday?,
    startNoYear: Boolean,
): Int =
    birthday?.year
        ?: if (startNoYear) DEFAULT_PICKER_YEAR else LocalDate.now().year

/**
 * Итог диалога: выбор пользователя, а без выбора — исходное значение с подставленным
 * годом показа. В режиме «без года» год обнуляется, что бы ни было выбрано в календаре.
 * null — выбирать нечего, подтверждение ничего не делает.
 */
internal fun pickerBirthdayResult(
    selectedDateMillis: Long?,
    birthday: Birthday?,
    initialYear: Int,
    noYear: Boolean,
): Birthday? {
    val date =
        selectedDateMillis
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            ?: birthday?.let { LocalDate.of(initialYear, it.month, it.day) }
            ?: return null
    return Birthday(
        day = date.dayOfMonth,
        month = date.monthValue,
        year = if (noYear) null else date.year,
    )
}

private const val DEFAULT_PICKER_YEAR = 2000
