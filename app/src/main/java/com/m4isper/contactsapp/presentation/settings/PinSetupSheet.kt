package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.components.AppBottomSheet
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Установка PIN на том же пад, которым затем разблокируются: два шага —
 * придумать код и повторить его. Длину выбирает пользователь, поэтому переход
 * между шагами по клавише-галочке, а сохранение — по «Готово» при совпадении.
 */
@Composable
internal fun PinSetupSheet(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.app_lock_pin),
) {
    val spacing = LocalSpacing.current
    val haptic = LocalHapticFeedback.current
    val entry = remember { SetupPinEntry() }

    val tap: () -> Unit = { haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap) }
    val error: () -> Unit = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("app_lock_pin_dialog"),
    ) {
        Text(
            text =
                stringResource(
                    if (entry.stage == SetupStage.FIRST) {
                        R.string.app_lock_setup_step_first
                    } else {
                        R.string.app_lock_setup_step_repeat
                    },
                ),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.xs),
        )
        LockPinDots(
            entered = entry.pin.length,
            target = if (entry.stage == SetupStage.REPEAT) entry.first.length else 0,
        )
        if (entry.shortFlash && entry.stage == SetupStage.FIRST) {
            Text(
                text = stringResource(R.string.app_lock_length_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .padding(top = spacing.s)
                        // Подсказка читается вслух и повторяется при короткой попытке.
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (entry.mismatch) {
            Text(
                text = stringResource(R.string.app_lock_pin_mismatch),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .padding(top = spacing.s)
                        // Несовпадение появляется динамически: озвучиваем его.
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        // Пад скроллится при нужде, «Готово» закреплено и всегда доступно —
        // та же схема, что в листе времени напоминания (issue 68).
        Box(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            LockPinPad(
                enabled = true,
                onDigit = { digit ->
                    tap()
                    entry.append(digit)
                },
                onBackspace = {
                    tap()
                    entry.delete()
                },
                onBiometricRetry = null,
                onConfirmEntry =
                    if (entry.stage == SetupStage.FIRST) {
                        {
                            if (!entry.confirmFirst()) {
                                error()
                            } else {
                                tap()
                            }
                        }
                    } else {
                        null
                    },
            )
        }
        if (entry.stage == SetupStage.REPEAT) {
            Button(
                onClick = {
                    if (entry.confirmPair()) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onConfirm(entry.first)
                    } else {
                        error()
                    }
                },
                enabled = entry.done,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = spacing.xs, bottom = spacing.s)
                        .testTag("app_lock_pin_confirm"),
            ) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}
