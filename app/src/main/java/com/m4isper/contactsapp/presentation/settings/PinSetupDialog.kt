package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R

/**
 * Диалог настройки PIN-кода: код и его повтор. Next на клавиатуре переводит
 * на повтор, Done отправляет валидную пару, несовпадение озвучивается.
 */
@Composable
internal fun PinSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.app_lock_pin),
) {
    val repeatFocus = remember { FocusRequester() }
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val mismatch = repeat.isNotEmpty() && pin != repeat
    val valid = pin.length >= MIN_PIN_LENGTH && pin == repeat

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                    keyboardActions = KeyboardActions(onNext = { repeatFocus.requestFocus() }),
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
                    keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(pin) }),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .focusRequester(repeatFocus)
                            .testTag("app_lock_pin_repeat_field"),
                )
                if (mismatch) {
                    Text(
                        text = stringResource(R.string.app_lock_pin_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        // Несовпадение появляется динамически: озвучиваем его.
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pin) },
                enabled = valid,
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
