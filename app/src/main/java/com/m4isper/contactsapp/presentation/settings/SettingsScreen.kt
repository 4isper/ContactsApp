package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Настройки: тема, сортировка списка и его плотность. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val spacing = LocalSpacing.current

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
            HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.m))
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
            HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.m))
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
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(modifier = Modifier.padding(vertical = spacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
        )
        Column(modifier = Modifier.selectableGroup()) {
            content()
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
                .padding(horizontal = spacing.m, vertical = spacing.s)
                .testTag(testTag)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ),
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

private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

private fun ContactSortOrder.labelRes(): Int =
    when (this) {
        ContactSortOrder.BY_NAME -> R.string.settings_sort_by_name
        ContactSortOrder.BY_FAMILY -> R.string.settings_sort_by_family
    }

private fun ListDensity.labelRes(): Int =
    when (this) {
        ListDensity.COMFORTABLE -> R.string.settings_density_comfortable
        ListDensity.COMPACT -> R.string.settings_density_compact
    }
