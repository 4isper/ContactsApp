package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R

/** Диалог настройки PIN-кода: код и его повтор. */
@Composable
internal fun PinSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val mismatch = repeat.isNotEmpty() && pin != repeat

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_lock_pin)) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(MAX_PIN_LENGTH) },
                    label = { Text(stringResource(R.string.app_lock_pin)) },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Next,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("app_lock_pin_field"),
                )
                OutlinedTextField(
                    value = repeat,
                    onValueChange = { repeat = it.filter(Char::isDigit).take(MAX_PIN_LENGTH) },
                    label = { Text(stringResource(R.string.app_lock_pin_repeat)) },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("app_lock_pin_repeat_field"),
                )
                if (mismatch) {
                    Text(
                        text = stringResource(R.string.app_lock_pin_mismatch),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= MIN_PIN_LENGTH && pin == repeat,
                modifier = Modifier.testTag("app_lock_pin_confirm"),
            ) {
                Text(stringResource(R.string.editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag("app_lock_pin_dialog"),
    )
}

/** Минимальная длина PIN-кода. */
internal const val MIN_PIN_LENGTH = 4

/** Максимальная длина PIN-кода. */
internal const val MAX_PIN_LENGTH = 8
