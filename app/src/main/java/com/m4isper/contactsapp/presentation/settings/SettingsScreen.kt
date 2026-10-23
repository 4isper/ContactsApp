package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.presentation.common.account.contactAccountTitle
import com.m4isper.contactsapp.presentation.common.account.testKey
import com.m4isper.contactsapp.presentation.common.message
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Настройки: тема, сортировка списка и его плотность. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onRestartRequired: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val spacing = LocalSpacing.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = spacing.s),
        ) {
            uiState.error?.let { error ->
                Text(
                    text = error.message(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.s),
                )
            }
            SettingsSection(title = stringResource(R.string.settings_theme)) {
                ThemeMode.entries.forEach { mode ->
                    SettingsOption(
                        label = stringResource(mode.labelRes()),
                        selected = settings.themeMode == mode,
                        onSelect = { viewModel.onThemeModeSelected(mode) },
                        testTag = "theme_${mode.name}",
                    )
                }
            }
            SettingsSection(title = stringResource(R.string.settings_sort_order)) {
                ContactSortOrder.entries.forEach { order ->
                    SettingsOption(
                        label = stringResource(order.labelRes()),
                        selected = settings.sortOrder == order,
                        onSelect = { viewModel.onSortOrderSelected(order) },
                        testTag = "sort_${order.name}",
                    )
                }
            }
            SettingsSection(title = stringResource(R.string.settings_list_density)) {
                ListDensity.entries.forEach { density ->
                    SettingsOption(
                        label = stringResource(density.labelRes()),
                        selected = settings.listDensity == density,
                        onSelect = { viewModel.onListDensitySelected(density) },
                        testTag = "density_${density.name}",
                    )
                }
            }
            SettingsSection(title = stringResource(R.string.settings_favorites)) {
                SettingsSwitch(
                    label = stringResource(R.string.settings_favorites_first),
                    description = stringResource(R.string.settings_favorites_first_desc),
                    checked = settings.favoritesFirst,
                    onCheckedChange = viewModel::onFavoritesFirstChanged,
                    testTag = "favorites_first_switch",
                )
            }
            if (uiState.accounts.isNotEmpty()) {
                SettingsSection(title = stringResource(R.string.settings_default_account)) {
                    uiState.accounts.forEach { account ->
                        SettingsOption(
                            label = contactAccountTitle(account),
                            selected = settings.defaultContactAccount == account,
                            onSelect = { viewModel.onDefaultAccountSelected(account) },
                            testTag = "default_account_${account.testKey()}",
                        )
                    }
                }
            }
            SettingsSection(title = stringResource(R.string.settings_language)) {
                APP_LANGUAGES.forEach { (tag, label) ->
                    SettingsOption(
                        label = label ?: stringResource(R.string.settings_language_system),
                        selected = settings.appLanguage == tag,
                        onSelect = {
                            viewModel.onLanguageSelected(tag)
                            // Язык применяется при создании Activity: окно пересоздаёт хост.
                            onRestartRequired()
                        },
                        testTag = "language_${tag.ifBlank { "system" }}",
                    )
                }
            }
            SettingsSection(title = stringResource(R.string.settings_appearance)) {
                SettingsSwitch(
                    label = stringResource(R.string.settings_dynamic_color),
                    description = stringResource(R.string.settings_dynamic_color_desc),
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::onDynamicColorChanged,
                    testTag = "dynamic_color_switch",
                )
                SettingsSwitch(
                    label = stringResource(R.string.settings_show_photos),
                    description = stringResource(R.string.settings_show_photos_desc),
                    checked = settings.showPhotos,
                    onCheckedChange = viewModel::onShowPhotosChanged,
                    testTag = "show_photos_switch",
                )
            }
            SettingsSection(title = stringResource(R.string.settings_privacy)) {
                SettingsSwitch(
                    label = stringResource(R.string.settings_app_lock),
                    description = stringResource(R.string.settings_app_lock_desc),
                    checked = settings.appLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinDialog = true
                        } else {
                            viewModel.onAppLockChanged(false, "")
                        }
                    },
                    testTag = "app_lock_switch",
                )
                SettingsSwitch(
                    label = stringResource(R.string.settings_secure_window),
                    description = stringResource(R.string.settings_secure_window_desc),
                    checked = settings.secureWindow,
                    onCheckedChange = viewModel::onSecureWindowChanged,
                    testTag = "secure_window_switch",
                )
            }
            RemindersSection(settings = settings, viewModel = viewModel)
            SettingsSection(title = stringResource(R.string.settings_about)) {
                SettingsInfo(
                    label = stringResource(R.string.settings_version, appVersion(context)),
                    testTag = "app_version",
                )
                SettingsInfo(
                    label = stringResource(R.string.settings_privacy_note),
                    testTag = "app_privacy_note",
                )
            }
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.testTag("reset_settings_button"),
            ) {
                Text(
                    text = stringResource(R.string.settings_reset),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onConfirm = { pin ->
                showPinDialog = false
                viewModel.onAppLockChanged(true, pin)
            },
            onDismiss = { showPinDialog = false },
        )
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset),
            text = stringResource(R.string.settings_reset_desc),
            confirmLabel = stringResource(R.string.action_reset),
            onConfirm = {
                showResetDialog = false
                viewModel.onResetSettings()
                onRestartRequired()
            },
            onDismiss = { showResetDialog = false },
            danger = true,
            confirmTestTag = "confirm_reset_settings",
        )
    }
}

/** Варианты предупреждения заранее: за сколько дней напомнить о празднике. */
private val REMINDER_LEAD_OPTIONS = listOf(1, 3, 7, 14)

/** Напоминания: переключатель и срок предупреждения. */
@Composable
private fun RemindersSection(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    SettingsSection(title = stringResource(R.string.settings_reminders)) {
        SettingsSwitch(
            label = stringResource(R.string.settings_reminders),
            description = stringResource(R.string.settings_reminders_desc),
            checked = settings.remindersEnabled,
            onCheckedChange = { enabled ->
                viewModel.onRemindersChanged(enabled, settings.reminderLeadDays)
            },
            testTag = "reminders_switch",
        )
        if (settings.remindersEnabled) {
            REMINDER_LEAD_OPTIONS.forEach { days ->
                SettingsOption(
                    label = stringResource(R.string.settings_reminder_lead_value, days),
                    selected = settings.reminderLeadDays == days,
                    onSelect = { viewModel.onRemindersChanged(true, days) },
                    testTag = "reminder_lead_$days",
                )
            }
        }
    }
}

/** Языки приложения: пустой код — язык системы. */
private val APP_LANGUAGES =
    listOf(
        "" to null,
        "en" to "English",
        "ru" to "Русский",
    )

/** Версия приложения для раздела «О программе». */
private fun appVersion(context: android.content.Context): String =
    runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName.orEmpty()
    }.getOrDefault("")

@Composable
private fun SettingsInfo(
    label: String,
    testTag: String,
) {
    val spacing = LocalSpacing.current
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .testTag(testTag),
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column(modifier = Modifier.padding(vertical = spacing.s)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            )
            Column(modifier = Modifier.selectableGroup()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = spacing.m),
        )
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

private fun ContactSortOrder.labelRes(): Int =
    when (this) {
        ContactSortOrder.BY_NAME -> R.string.settings_sort_by_name
        ContactSortOrder.BY_NAME_DESC -> R.string.settings_sort_by_name_desc
        ContactSortOrder.BY_FAMILY -> R.string.settings_sort_by_family
        ContactSortOrder.BY_COMPANY -> R.string.settings_sort_by_company
        ContactSortOrder.RECENTLY_ADDED -> R.string.settings_sort_recently_added
    }

private fun ListDensity.labelRes(): Int =
    when (this) {
        ListDensity.COMFORTABLE -> R.string.settings_density_comfortable
        ListDensity.COMPACT -> R.string.settings_density_compact
    }
