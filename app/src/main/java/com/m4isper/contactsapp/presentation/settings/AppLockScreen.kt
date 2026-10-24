package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

/** Экран ввода PIN-кода: приложение открыт, но защищён. */
@Composable
internal fun AppLockScreen(
    onUnlock: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
    lockedFor: Duration = Duration.ZERO,
) {
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var remaining by remember(lockedFor) { mutableStateOf(lockedFor) }

    // Обратный отсчёт блокировки: пока ждём, сообщаем, сколько осталось.
    LaunchedEffect(lockedFor) {
        while (remaining > Duration.ZERO) {
            delay(COUNTDOWN_STEP_MILLIS)
            remaining -= Duration.ofMillis(COUNTDOWN_STEP_MILLIS)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(spacing.xl),
        ) {
            Text(
                text = stringResource(R.string.app_lock_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.app_lock_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.s, bottom = spacing.l),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter(Char::isDigit).take(MAX_PIN_LENGTH)
                    failed = false
                },
                label = { Text(stringResource(R.string.app_lock_pin)) },
                singleLine = true,
                isError = failed,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("app_lock_pin_field"),
            )
            if (failed) {
                Text(
                    text = stringResource(R.string.app_lock_wrong_pin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
            if (remaining > Duration.ZERO) {
                Text(
                    text = stringResource(R.string.app_lock_too_many_attempts, remaining.seconds.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
            Button(
                onClick = {
                    checking = true
                    scope.launch {
                        failed = !onUnlock(pin)
                        checking = false
                    }
                },
                enabled = pin.length >= MIN_PIN_LENGTH && !checking && remaining == Duration.ZERO,
                modifier =
                    Modifier
                        .padding(top = spacing.m)
                        .testTag("app_lock_unlock_button"),
            ) {
                Text(stringResource(R.string.app_lock_unlock))
            }
        }
    }
}

private const val COUNTDOWN_STEP_MILLIS = 1_000L
