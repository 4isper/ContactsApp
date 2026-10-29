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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DensityMedium
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
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
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.presentation.common.account.contactAccountTitle
import com.m4isper.contactsapp.presentation.common.account.testKey
import com.m4isper.contactsapp.presentation.common.message
import com.m4isper.contactsapp.ui.adaptive.ReadingColumn
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Листы выбора, которые открывает свёрнутая строка настроек. */
private enum class SettingsSheet {
    THEME,
    DENSITY,
    LANGUAGE,
    SORT,
    ACCOUNT,
    LICENSES,
}

/** Настройки: оформление, поведение, безопасность и служебные разделы. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRestartRequired: () -> Unit = {},
    viewModel: SettingsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val spacing = LocalSpacing.current
    val systemLanguage = stringResource(R.string.settings_language_system)
    val languageValue = LANGUAGE_LABELS[settings.appLanguage] ?: systemLanguage
    var openSheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var showPinSheet by remember { mutableStateOf(false) }
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
        // Читаемая колонка: на планшете список настроек центрирован, а не растянут на всю ширину.
        ReadingColumn(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
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
                SettingsSection(title = stringResource(R.string.settings_appearance)) {
                    SettingsChoiceRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_theme),
                        value = stringResource(settings.themeMode.labelRes()),
                        onClick = { openSheet = SettingsSheet.THEME },
                        testTag = "settings_theme",
                    )
                    SettingsChoiceRow(
                        icon = Icons.Filled.DensityMedium,
                        title = stringResource(R.string.settings_list_density),
                        value = stringResource(settings.listDensity.labelRes()),
                        onClick = { openSheet = SettingsSheet.DENSITY },
                        testTag = "settings_density",
                    )
                    SettingsChoiceRow(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.settings_language),
                        value = languageValue,
                        onClick = { openSheet = SettingsSheet.LANGUAGE },
                        testTag = "settings_language",
                    )
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
                SettingsSection(title = stringResource(R.string.settings_behavior)) {
                    SettingsChoiceRow(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        title = stringResource(R.string.settings_sort_order),
                        value = stringResource(settings.sortOrder.labelRes()),
                        onClick = { openSheet = SettingsSheet.SORT },
                        testTag = "settings_sort",
                    )
                    SettingsSwitch(
                        label = stringResource(R.string.settings_favorites_first),
                        description = stringResource(R.string.settings_favorites_first_desc),
                        checked = settings.favoritesFirst,
                        onCheckedChange = viewModel::onFavoritesFirstChanged,
                        testTag = "favorites_first_switch",
                    )
                    if (uiState.accounts.size > 1) {
                        SettingsChoiceRow(
                            icon = Icons.Filled.AccountCircle,
                            title = stringResource(R.string.settings_default_account),
                            value = contactAccountTitle(settings.defaultContactAccount),
                            onClick = { openSheet = SettingsSheet.ACCOUNT },
                            testTag = "settings_default_account",
                        )
                    }
                }
                RemindersSection(settings = settings, viewModel = viewModel)
                SettingsSection(title = stringResource(R.string.settings_security)) {
                    SettingsSwitch(
                        label = stringResource(R.string.settings_app_lock),
                        description = stringResource(R.string.settings_app_lock_desc),
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showPinSheet = true
                            } else {
                                viewModel.onAppLockChanged(false, "")
                            }
                        },
                        testTag = "app_lock_switch",
                    )
                    SettingsPinState(
                        enabled = settings.appLockEnabled,
                        onChangePin = { showPinSheet = true },
                    )
                    if (biometricsAvailable(context)) {
                        SettingsSwitch(
                            label = stringResource(R.string.settings_app_lock_biometrics),
                            description = stringResource(R.string.settings_app_lock_biometrics_desc),
                            checked = settings.appLockBiometrics,
                            onCheckedChange = viewModel::onAppLockBiometricsChanged,
                            testTag = "app_lock_biometrics_switch",
                        )
                    }
                    if (settings.appLockEnabled) {
                        // Восстановления PIN нет: пользователь должен знать цену до блокировки.
                        Text(
                            text = stringResource(R.string.settings_forgot_pin),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .padding(horizontal = spacing.m, vertical = spacing.xs)
                                    .testTag("forgot_pin_note"),
                        )
                    }
                    SettingsSwitch(
                        label = stringResource(R.string.settings_secure_window),
                        description = stringResource(R.string.settings_secure_window_desc),
                        checked = settings.secureWindow,
                        onCheckedChange = viewModel::onSecureWindowChanged,
                        testTag = "secure_window_switch",
                    )
                }
                SettingsDangerZone(onReset = { showResetDialog = true })
                SettingsAboutSection(
                    version = appVersion(context),
                    onShowLicenses = { openSheet = SettingsSheet.LICENSES },
                )
            }
        }
    }

    SettingsSheetHost(
        sheet = openSheet,
        settings = settings,
        accounts = uiState.accounts,
        systemLanguage = systemLanguage,
        onDismiss = { openSheet = null },
        viewModel = viewModel,
        onRestartRequired = onRestartRequired,
    )

    if (showPinSheet) {
        PinSetupSheet(
            onConfirm = { pin ->
                showPinSheet = false
                viewModel.onAppLockChanged(true, pin)
            },
            onDismiss = { showPinSheet = false },
            title =
                stringResource(
                    if (settings.appLockEnabled) {
                        R.string.settings_app_lock_change_pin
                    } else {
                        R.string.app_lock_pin
                    },
                ),
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

/** Один лист выбора за раз: строка применения закрывает его тем же нажатием. */
@Composable
private fun SettingsSheetHost(
    sheet: SettingsSheet?,
    settings: AppSettings,
    accounts: List<ContactAccount>,
    systemLanguage: String,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel,
    onRestartRequired: () -> Unit,
) {
    when (sheet) {
        null -> Unit
        SettingsSheet.THEME ->
            SettingsChoiceSheet(
                title = stringResource(R.string.settings_theme),
                options =
                    ThemeMode.entries.map { mode ->
                        SettingsChoice(mode, stringResource(mode.labelRes()), "theme_${mode.name}")
                    },
                selected = settings.themeMode,
                onSelect = {
                    viewModel.onThemeModeSelected(it)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        SettingsSheet.DENSITY ->
            SettingsChoiceSheet(
                title = stringResource(R.string.settings_list_density),
                options =
                    ListDensity.entries.map { density ->
                        SettingsChoice(density, stringResource(density.labelRes()), "density_${density.name}")
                    },
                selected = settings.listDensity,
                onSelect = {
                    viewModel.onListDensitySelected(it)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        SettingsSheet.LANGUAGE ->
            SettingsChoiceSheet(
                title = stringResource(R.string.settings_language),
                options =
                    APP_LANGUAGES.map { (tag, label) ->
                        SettingsChoice(tag, label ?: systemLanguage, "language_${tag.ifBlank { "system" }}")
                    },
                selected = settings.appLanguage,
                onSelect = {
                    viewModel.onLanguageSelected(it)
                    onDismiss()
                    // Язык применяется при создании Activity: окно пересоздаёт хост.
                    // На Android 13+ система пересоздаёт его сама после setApplicationLocales.
                    if (viewModel.languageChangeRestarts) onRestartRequired()
                },
                onDismiss = onDismiss,
            )
        SettingsSheet.SORT ->
            SettingsChoiceSheet(
                title = stringResource(R.string.settings_sort_order),
                options =
                    ContactSortOrder.entries.map { order ->
                        SettingsChoice(order, stringResource(order.labelRes()), "sort_${order.name}")
                    },
                selected = settings.sortOrder,
                onSelect = {
                    viewModel.onSortOrderSelected(it)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        SettingsSheet.ACCOUNT ->
            SettingsChoiceSheet(
                title = stringResource(R.string.settings_default_account),
                options =
                    accounts.map { account ->
                        SettingsChoice(account, contactAccountTitle(account), "default_account_${account.testKey()}")
                    },
                selected = settings.defaultContactAccount,
                onSelect = {
                    viewModel.onDefaultAccountSelected(it)
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        SettingsSheet.LICENSES -> LicensesSheet(onDismiss = onDismiss)
    }
}

/** Языки приложения: пустой код — язык системы. Подписи на самих языках. */
private val APP_LANGUAGES =
    listOf(
        "" to null,
        "en" to "English",
        "ru" to "Русский",
        "es" to "Español",
        "de" to "Deutsch",
        "fr" to "Français",
        "pt-BR" to "Português (Brasil)",
    )

/** Подписи языков, выбранных явно. */
private val LANGUAGE_LABELS = APP_LANGUAGES.mapNotNull { (tag, label) -> label?.let { tag to it } }.toMap()

/** Версия приложения для раздела «О программе». */
private fun appVersion(context: android.content.Context): String =
    runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName.orEmpty()
    }.getOrDefault("")

@Composable
internal fun SettingsSection(
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
internal fun SettingsOption(
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
internal fun SettingsSwitch(
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
