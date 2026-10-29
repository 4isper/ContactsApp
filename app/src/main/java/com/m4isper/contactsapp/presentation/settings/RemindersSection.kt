package com.m4isper.contactsapp.presentation.settings

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ReminderTime
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings
import com.m4isper.contactsapp.ui.components.AppBottomSheet
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Варианты предупреждения заранее: за сколько дней напомнить о празднике. */
private val REMINDER_LEAD_OPTIONS = listOf(1, 3, 7, 14)

/** Напоминания: переключатель, срок предупреждения и время суток. */
@Composable
internal fun RemindersSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    var appNotificationsOn by remember { mutableStateOf(true) }
    var channelBlocked by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Возврат из системных настроек: пересчитываем, доставляются ли уведомления.
    fun refreshDelivery() {
        appNotificationsOn = appNotificationsEnabled(context)
        channelBlocked = remindersChannelBlocked(context)
    }

    LifecycleResumeEffect(Unit) {
        refreshDelivery()
        onPauseOrDispose { /* no-op */ }
    }

    val deliverable =
        remindersEffectivelyOn(
            enabled = settings.remindersEnabled,
            appNotificationsEnabled = appNotificationsOn,
            channelBlocked = channelBlocked,
        )

    SettingsSection(title = stringResource(R.string.settings_reminders)) {
        RemindersEnableSwitch(
            settings = settings,
            viewModel = viewModel,
            context = context,
            appNotificationsOn = appNotificationsOn,
            channelBlocked = channelBlocked,
            onRefreshDelivery = ::refreshDelivery,
        )
        RemindersDeliveryWarnings(
            visible = settings.remindersEnabled && (channelBlocked || !appNotificationsOn),
            onOpenSettings = { openAppSettings(context) },
        )
        RemindersLeadOptions(
            visible = deliverable,
            selectedDays = settings.reminderLeadDays,
            settings = settings,
            viewModel = viewModel,
        )
        if (deliverable) {
            SettingsChoiceRow(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.settings_reminder_time),
                value = settings.reminderTime.format(context),
                onClick = { showTimePicker = true },
                testTag = "reminder_time",
            )
        }
    }

    if (showTimePicker) {
        ReminderTimeSheet(
            initial = settings.reminderTime,
            onDone = { time ->
                showTimePicker = false
                viewModel.onRemindersChanged(true, settings.reminderLeadDays, time)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/**
 * Переключатель напоминаний: настройка применяется только после ответа системы
 * на запрос разрешения, поэтому решение живёт здесь, рядом с запуском запроса.
 */
@Composable
private fun RemindersEnableSwitch(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    context: android.content.Context,
    appNotificationsOn: Boolean,
    channelBlocked: Boolean,
    onRefreshDelivery: () -> Unit,
) {
    var permissionDenied by remember { mutableStateOf(false) }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            // Настройка применяется только после ответа системы, не до него.
            if (granted) {
                permissionDenied = false
                viewModel.onRemindersChanged(true, settings.reminderLeadDays, settings.reminderTime)
            } else {
                permissionDenied = true
            }
        }

    SettingsSwitch(
        label = stringResource(R.string.settings_reminders_enable),
        description = stringResource(R.string.settings_reminders_desc),
        checked =
            remindersEffectivelyOn(
                enabled = settings.remindersEnabled,
                appNotificationsEnabled = appNotificationsOn,
                channelBlocked = channelBlocked,
            ),
        onCheckedChange = { enabled ->
            permissionDenied = false
            when {
                // Выключение всегда применяется сразу.
                !enabled -> viewModel.onRemindersChanged(false, settings.reminderLeadDays, settings.reminderTime)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS) ->
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> {
                    onRefreshDelivery()
                    if (appNotificationsOn && !channelBlocked) {
                        viewModel.onRemindersChanged(true, settings.reminderLeadDays, settings.reminderTime)
                    } else {
                        permissionDenied = true
                    }
                }
            }
        },
        testTag = "reminders_switch",
    )
    if (permissionDenied) {
        RemindersWarning(
            text = stringResource(R.string.settings_reminders_permission),
            onOpenSettings = { openAppSettings(context) },
        )
    }
}

/** Время в местном формате: 9:00 AM или 09:00 — как устроены часы у пользователя. */
private fun ReminderTime.format(context: android.content.Context): String =
    LocalTime
        .of(hour, minute)
        .format(
            DateTimeFormatter
                .ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(context.resources.configuration.locales[0]),
        )

/** Лист выбора времени: пикер скроллится при нужде, «Готово» закреплено и всегда видно. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeSheet(
    initial: ReminderTime,
    onDone: (ReminderTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val timePickerState =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
    AppBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("reminder_time_sheet"),
    ) {
        Text(
            text = stringResource(R.string.settings_reminder_time),
            style = MaterialTheme.typography.titleLarge,
        )
        Box(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            TimePicker(
                state = timePickerState,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(vertical = spacing.s),
            )
        }
        Button(
            onClick = { onDone(ReminderTime(timePickerState.hour, timePickerState.minute)) },
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = spacing.xs, bottom = spacing.s)
                    .testTag("reminder_time_done"),
        ) {
            Text(stringResource(R.string.action_done))
        }
    }
}

/** Предупреждение о недоставляемых уведомлениях с переходом в настройки. */
@Composable
private fun RemindersDeliveryWarnings(
    visible: Boolean,
    onOpenSettings: () -> Unit,
) {
    if (!visible) return
    RemindersWarning(
        text = stringResource(R.string.settings_reminders_blocked),
        onOpenSettings = onOpenSettings,
    )
}

/** Варианты срока предупреждения: видны, только когда напоминания реально доставляются. */
@Composable
private fun RemindersLeadOptions(
    visible: Boolean,
    selectedDays: Int,
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    if (!visible) return
    REMINDER_LEAD_OPTIONS.forEach { days ->
        SettingsOption(
            label = pluralStringResource(R.plurals.settings_reminder_lead_value, days, days),
            selected = selectedDays == days,
            onSelect = { viewModel.onRemindersChanged(true, days, settings.reminderTime) },
            testTag = "reminder_lead_$days",
        )
    }
}

/** Предупреждение под переключателем: пояснение и переход в настройки. */
@Composable
private fun RemindersWarning(
    text: String,
    onOpenSettings: () -> Unit,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp),
    )
    TextButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.permission_open_settings))
    }
}
