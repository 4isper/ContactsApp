package com.m4isper.contactsapp.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.components.AppLoadingIndicator
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import com.m4isper.contactsapp.ui.theme.PillShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

private const val COUNTDOWN_STEP_MILLIS = 1_000L

/**
 * Экран блокировки: точки введённых цифр и цифровой пад вместо системной клавиатуры.
 * Известная длина PIN (сохраняется установкой) даёт фиксированную цель и отправку
 * на последней цифре; установки до обновления работают в легаси-режиме — растущие
 * точки, подсказка «4–8 цифр» и кнопка разблокировки. Когда биометрия включена
 * и доступна, системный промпт показывается сам, а ключ с отпечатком повторяет его.
 */
@Composable
internal fun AppLockScreen(
    onUnlock: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
    onBiometricUnlock: () -> Unit = {},
    biometricsEnabled: Boolean = false,
    pinLength: Int = 0,
    lockedFor: Duration = Duration.ZERO,
) {
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val entry =
        remember(pinLength) {
            LockPinEntry(targetLength = pinLength.takeIf { it in MIN_PIN_LENGTH..MAX_PIN_LENGTH })
        }
    var remaining by remember(lockedFor) { mutableStateOf(lockedFor) }

    // Биометрия активна, только если она включена в настройках, доступна на устройстве
    // и хост умеет строить промпт (FragmentActivity). Иначе — только пад.
    val activity = LocalActivity.current
    val context = LocalContext.current
    val prompt = remember(activity) { BiometricUnlock.from(activity) }
    val biometricsOnDevice = remember(context) { biometricsAvailable(context) }
    val biometricActive = biometricsEnabled && prompt != null && biometricsOnDevice

    val locked = remaining > Duration.ZERO

    val check: () -> Unit = {
        if (entry.beginCheck()) {
            scope.launch {
                val success = onUnlock(entry.pin)
                if (!success) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                entry.finish(success)
            }
        } else {
            // Легаси-режим, короткий ввод: не молчим, а объясняем требование.
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val appendDigit: (Int) -> Unit = { digit ->
        if (!locked) {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            // Известная длина: отправка на последней цифре, без лишнего нажатия.
            if (entry.append(digit)) check()
        }
    }
    val deleteDigit: () -> Unit = {
        if (!locked) {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            entry.delete()
        }
    }

    val showPrompt: () -> Unit = {
        prompt?.authenticate(
            onUnlock = onBiometricUnlock,
            onPinFallback = { },
        )
    }
    if (biometricActive) {
        // Ключ по активности: промпт показывается и на входе в блокировку,
        // и когда настройки донастроили биометрию уже под закрытым гейтом.
        LifecycleResumeEffect(biometricActive) {
            showPrompt()
            onPauseOrDispose { }
        }
    }

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
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.xl),
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
            LockEntryBody(
                entry = entry,
                locked = locked,
                remaining = remaining,
                appendDigit = appendDigit,
                deleteDigit = deleteDigit,
                check = check,
                onBiometricRetry = if (biometricActive) showPrompt else null,
            )
        }
    }
}

/** Сколько секунд блокировки осталось: дробные значения округляются вверх. */
internal fun lockoutSecondsLeft(remaining: Duration): Int =
    ((remaining.toMillis() + COUNTDOWN_STEP_MILLIS - 1) / COUNTDOWN_STEP_MILLIS).toInt()

/** Сообщения и управление под заголовком: подсказка, ошибки, блокировка, пад и кнопка. */
@Composable
private fun LockEntryBody(
    entry: LockPinEntry,
    locked: Boolean,
    remaining: Duration,
    appendDigit: (Int) -> Unit,
    deleteDigit: () -> Unit,
    check: () -> Unit,
    onBiometricRetry: (() -> Unit)?,
) {
    val spacing = LocalSpacing.current
    LockPinDots(
        entered = entry.pin.length,
        target = entry.targetLength ?: 0,
    )
    if (entry.targetLength == null) {
        // Легаси-установка: длину не знаем — держим подсказку на виду,
        // при короткой попытке она подсвечивается ошибкой.
        Text(
            text = stringResource(R.string.app_lock_length_hint),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (entry.failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier =
                Modifier
                    .padding(top = spacing.s)
                    // Подсказка читается вслух и повторяется при короткой попытке.
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("app_lock_length_hint"),
        )
    }
    if (entry.failed && entry.targetLength != null) {
        Text(
            text = stringResource(R.string.app_lock_wrong_pin),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .padding(top = spacing.xs)
                    // Появление и повторное появление ошибки читается TalkBack.
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (locked) {
        Text(
            text =
                stringResource(
                    R.string.app_lock_too_many_attempts,
                    lockoutSecondsLeft(remaining),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .padding(top = spacing.xs)
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    if (entry.checking) {
        AppLoadingIndicator(
            modifier =
                Modifier
                    .padding(top = spacing.m)
                    .size(spacing.xxl),
        )
    }
    LockPinPad(
        enabled = !locked && !entry.checking,
        onDigit = appendDigit,
        onBackspace = deleteDigit,
        onBiometricRetry = onBiometricRetry,
    )
    if (entry.targetLength == null) {
        // Легаси-режим: длина неизвестна, отправка вручную.
        Button(
            onClick = check,
            enabled = !entry.checking && !locked,
            shape = PillShape,
            modifier =
                Modifier
                    .padding(top = spacing.m)
                    .testTag("app_lock_unlock_button"),
        ) {
            Text(stringResource(R.string.app_lock_unlock))
        }
    }
}
