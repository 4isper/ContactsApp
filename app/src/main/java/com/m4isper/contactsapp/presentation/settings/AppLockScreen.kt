package com.m4isper.contactsapp.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.components.AppLoadingIndicator
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import com.m4isper.contactsapp.ui.theme.PillShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * Экран ввода PIN-кода: приложение открыто, но защищено. Контент скроллится и
 * учитывает клавиатуру, чтобы на малых экранах поле и кнопка оставались доступны.
 * Когда биометрия включена и доступна, сначала показывается системный промпт,
 * а поле PIN появляется по кнопке «Ввести PIN».
 */
@Composable
internal fun AppLockScreen(
    onUnlock: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
    onBiometricUnlock: () -> Unit = {},
    biometricsEnabled: Boolean = false,
    lockedFor: Duration = Duration.ZERO,
) {
    val spacing = LocalSpacing.current
    val scope = rememberCoroutineScope()
    val pinFocus = remember { FocusRequester() }
    var pin by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var remaining by remember(lockedFor) { mutableStateOf(lockedFor) }

    // Биометрия активна, только если она включена в настройках, доступна на устройстве
    // и хост умеет строить промпт (FragmentActivity). Иначе — прежний PIN-экран.
    val activity = LocalActivity.current
    val context = LocalContext.current
    val prompt = remember(activity) { BiometricUnlock.from(activity) }
    val biometricsOnDevice = remember(context) { biometricsAvailable(context) }
    val biometricActive = biometricsEnabled && prompt != null && biometricsOnDevice
    // Пока биометрия не подтверждена как активная, показываем PIN сразу;
    // после её появления промпт на экране, поле — по кнопке «Ввести PIN».
    var pinRevealed by remember { mutableStateOf(false) }
    val showPinEntry = !biometricActive || pinRevealed

    val showPrompt: () -> Unit = {
        prompt?.authenticate(
            onUnlock = onBiometricUnlock,
            onPinFallback = { pinRevealed = true },
        )
    }
    // Промпт показывается на каждый вход в блокировку: экран собирается заново после разблокировки.
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

    val locked = remaining > Duration.ZERO
    val unlock: () -> Unit = {
        // Один защищённый вызов: повторное Done или нажатие кнопки не дублирует проверку.
        if (pin.length >= MIN_PIN_LENGTH && !checking && !locked) {
            checking = true
            scope.launch {
                failed = !onUnlock(pin)
                if (failed) {
                    pin = ""
                    pinFocus.requestFocus()
                }
                checking = false
            }
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
                    .imePadding()
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
            if (showPinEntry) {
                LockPinEntry(
                    pin = pin,
                    onPinChange = {
                        pin = it.filter(Char::isDigit).take(MAX_PIN_LENGTH)
                        failed = false
                    },
                    failed = failed,
                    locked = locked,
                    remaining = remaining,
                    showPin = showPin,
                    onToggleShowPin = { showPin = !showPin },
                    onDone = unlock,
                    focusRequester = pinFocus,
                    checking = checking,
                )
            } else {
                LockBiometricEntry(
                    showPrompt = showPrompt,
                    onUsePin = { pinRevealed = true },
                )
            }
        }
    }
}

/** Ввод PIN: поле, сообщения об ошибке и блокировке, кнопка разблокировки. */
@Composable
private fun LockPinEntry(
    pin: String,
    onPinChange: (String) -> Unit,
    failed: Boolean,
    locked: Boolean,
    remaining: Duration,
    showPin: Boolean,
    onToggleShowPin: () -> Unit,
    onDone: () -> Unit,
    focusRequester: FocusRequester,
    checking: Boolean,
) {
    val spacing = LocalSpacing.current
    LockPinField(
        pin = pin,
        onPinChange = onPinChange,
        failed = failed,
        locked = locked,
        showPin = showPin,
        onToggleShowPin = onToggleShowPin,
        onDone = onDone,
        focusRequester = focusRequester,
    )
    if (failed) {
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
    if (checking) {
        AppLoadingIndicator(
            modifier =
                Modifier
                    .padding(top = spacing.m)
                    .size(spacing.xxl),
        )
    }
    Button(
        onClick = onDone,
        enabled = pin.length >= MIN_PIN_LENGTH && !checking && !locked,
        shape = PillShape,
        modifier =
            Modifier
                .padding(top = spacing.m)
                .testTag("app_lock_unlock_button"),
    ) {
        Text(stringResource(R.string.app_lock_unlock))
    }
}

/** Промпт уже показан; на случай его закрытия — ручной повтор и путь к PIN. */
@Composable
private fun LockBiometricEntry(
    showPrompt: () -> Unit,
    onUsePin: () -> Unit,
) {
    val spacing = LocalSpacing.current
    IconButton(
        onClick = showPrompt,
        modifier = Modifier.testTag("app_lock_biometrics_retry"),
    ) {
        Icon(
            imageVector = Icons.Filled.Fingerprint,
            contentDescription = stringResource(R.string.app_lock_retry_biometrics),
            modifier = Modifier.size(spacing.xxl),
        )
    }
    TextButton(onClick = onUsePin) {
        Text(stringResource(R.string.app_lock_use_pin))
    }
}

/** Сколько секунд блокировки осталось: дробные значения округляются вверх. */
internal fun lockoutSecondsLeft(remaining: Duration): Int =
    ((remaining.toMillis() + COUNTDOWN_STEP_MILLIS - 1) / COUNTDOWN_STEP_MILLIS).toInt()

/** Поле PIN-кода: маскировка с переключателем показа, Done отправляет проверку. */
@Composable
private fun LockPinField(
    pin: String,
    onPinChange: (String) -> Unit,
    failed: Boolean,
    locked: Boolean,
    showPin: Boolean,
    onToggleShowPin: () -> Unit,
    onDone: () -> Unit,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        value = pin,
        onValueChange = onPinChange,
        label = { Text(stringResource(R.string.app_lock_pin)) },
        singleLine = true,
        isError = failed,
        enabled = !locked,
        visualTransformation =
            if (showPin) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        trailingIcon = {
            IconButton(onClick = onToggleShowPin) {
                Icon(
                    imageVector = if (showPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription =
                        stringResource(
                            if (showPin) R.string.app_lock_hide_pin else R.string.app_lock_show_pin,
                        ),
                )
            }
        },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .testTag("app_lock_pin_field"),
    )
}

private const val COUNTDOWN_STEP_MILLIS = 1_000L
