package com.m4isper.contactsapp.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings

/** Варианты предупреждения заранее: за сколько дней напомнить о празднике. */
private val REMINDER_LEAD_OPTIONS = listOf(1, 3, 7, 14)

/** Напоминания: переключатель и срок предупреждения. */
@Composable
internal fun RemindersSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    var appNotificationsOn by remember { mutableStateOf(true) }
    var channelBlocked by remember { mutableStateOf(false) }

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

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            // Настройка применяется только после ответа системы, не до него.
            if (granted) {
                permissionDenied = false
                viewModel.onRemindersChanged(true, settings.reminderLeadDays)
            } else {
                permissionDenied = true
            }
        }

    SettingsSection(title = stringResource(R.string.settings_reminders)) {
        SettingsSwitch(
            label = stringResource(R.string.settings_reminders),
            description = stringResource(R.string.settings_reminders_desc),
            checked = deliverable,
            onCheckedChange = { enabled ->
                permissionDenied = false
                when {
                    // Выключение всегда применяется сразу.
                    !enabled -> viewModel.onRemindersChanged(false, settings.reminderLeadDays)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS) ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else -> {
                        refreshDelivery()
                        if (appNotificationsOn && !channelBlocked) {
                            viewModel.onRemindersChanged(true, settings.reminderLeadDays)
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
        RemindersDeliveryWarnings(
            visible = settings.remindersEnabled && (channelBlocked || !appNotificationsOn),
            onOpenSettings = { openAppSettings(context) },
        )
        RemindersLeadOptions(
            visible = deliverable,
            selectedDays = settings.reminderLeadDays,
            onSelect = { days -> viewModel.onRemindersChanged(true, days) },
        )
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
    onSelect: (Int) -> Unit,
) {
    if (!visible) return
    REMINDER_LEAD_OPTIONS.forEach { days ->
        SettingsOption(
            label = stringResource(R.string.settings_reminder_lead_value, days),
            selected = selectedDays == days,
            onSelect = { onSelect(days) },
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
