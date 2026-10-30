package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Минимальная длина PIN-кода. */
internal const val MIN_PIN_LENGTH = 4

/** Максимальная длина PIN-кода. */
internal const val MAX_PIN_LENGTH = 8

/** Диаметр точки индикатора ввода. */
private val DOT_SIZE = 12.dp

/** Размер клавиши пада: крупные тач-таргеты — смысл пада. */
private val PAD_KEY_SIZE = 72.dp

/** Ряды цифр пада: три строки по три, ноль — в нижнем ряду. */
private val PAD_DIGIT_ROWS = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))

/**
 * Точки введённых цифр: при известной длине — фиксированная цель с пустыми
 * местами, при неизвестной — ряд только из введённых. Для TalkBack вся строка
 * — одно состояние, без озвучивания каждой точки.
 */
@Composable
internal fun LockPinDots(
    entered: Int,
    target: Int,
) {
    val spacing = LocalSpacing.current
    val stateText =
        if (target > 0) {
            stringResource(R.string.app_lock_dots_state, entered, target)
        } else {
            pluralStringResource(R.plurals.app_lock_digits_entered, entered, entered)
        }
    val filledColor = MaterialTheme.colorScheme.onSurface
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        modifier =
            Modifier
                .padding(top = spacing.xs)
                .semantics(mergeDescendants = true) { stateDescription = stateText }
                .testTag("app_lock_dots"),
    ) {
        val count = if (target > 0) target else entered
        repeat(count) { index ->
            Box(
                modifier =
                    Modifier
                        .size(DOT_SIZE)
                        .clip(CircleShape)
                        .background(if (index < entered) filledColor else emptyColor),
            )
        }
    }
}

/** Цифровой пад: 1–9, ведущий слот (биометрия или галочка), ноль, backspace. */
@Composable
internal fun LockPinPad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometricRetry: (() -> Unit)?,
    onConfirmEntry: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = spacing.l),
    ) {
        PAD_DIGIT_ROWS.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
                row.forEach { digit ->
                    PadKey(
                        onClick = { onDigit(digit) },
                        enabled = enabled,
                        testTag = "app_lock_key_$digit",
                        modifier = Modifier.size(PAD_KEY_SIZE),
                    ) {
                        Text(text = digit.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            modifier = Modifier.padding(top = spacing.l),
        ) {
            when {
                onBiometricRetry != null ->
                    PadKey(
                        onClick = onBiometricRetry,
                        enabled = true,
                        testTag = "app_lock_biometrics_retry",
                        modifier = Modifier.size(PAD_KEY_SIZE),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = stringResource(R.string.app_lock_retry_biometrics),
                        )
                    }
                onConfirmEntry != null ->
                    PadKey(
                        onClick = onConfirmEntry,
                        enabled = true,
                        testTag = "app_lock_setup_next",
                        modifier = Modifier.size(PAD_KEY_SIZE),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.app_lock_setup_next),
                        )
                    }
                else -> Box(modifier = Modifier.size(PAD_KEY_SIZE))
            }
            PadKey(
                onClick = { onDigit(0) },
                enabled = enabled,
                testTag = "app_lock_key_0",
                modifier = Modifier.size(PAD_KEY_SIZE),
            ) {
                Text(text = "0", style = MaterialTheme.typography.titleLarge)
            }
            PadKey(
                onClick = onBackspace,
                enabled = enabled,
                testTag = "app_lock_key_delete",
                modifier = Modifier.size(PAD_KEY_SIZE),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.app_lock_key_delete),
                )
            }
        }
    }
}

/** Круглая клавиша пада: одинаковая форма и поверхность для цифр и служебных клавиш. */
@Composable
private fun PadKey(
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
