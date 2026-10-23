package com.m4isper.contactsapp.presentation.editor.dialogs

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.Birthday
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BirthdayPickerDialog(
    birthday: Birthday?,
    onConfirmed: (Birthday) -> Unit,
    onDismiss: () -> Unit,
    confirmTestTag: String = "editor_birthday_confirm",
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                birthday?.let { value ->
                    LocalDate
                        .of(value.year ?: DEFAULT_PICKER_YEAR, value.month, value.day)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date =
                            Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        onConfirmed(
                            Birthday(
                                day = date.dayOfMonth,
                                month = date.monthValue,
                                year = date.year,
                            ),
                        )
                    }
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
        DatePicker(state = pickerState, showModeToggle = false, title = null)
    }
}

private const val DEFAULT_PICKER_YEAR = 2000
