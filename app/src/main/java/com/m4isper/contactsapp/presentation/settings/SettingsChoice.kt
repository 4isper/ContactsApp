package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.components.AppBottomSheet
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Один вариант в листе выбора: значение, подпись и стабильный тег. */
internal data class SettingsChoice<T>(
    val value: T,
    val label: String,
    val testTag: String,
)

/** Свёрнутая настройка: иконка, название и текущее значение; лист выбора — по нажатию. */
@Composable
internal fun SettingsChoiceRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = spacing.xxl + spacing.xl)
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        SettingsLeadingIcon(icon)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.m),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("${testTag}_value"),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Лист выбора варианта: применяем и закрываем одним нажатием. */
@Composable
internal fun <T> SettingsChoiceSheet(
    title: String,
    options: List<SettingsChoice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    AppBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("settings_choice_sheet"),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Column(
            modifier =
                Modifier
                    .padding(top = spacing.s)
                    .selectableGroup(),
        ) {
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag(option.testTag)
                            .selectable(
                                selected = option.value == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option.value) },
                            ).heightIn(min = spacing.xxl + spacing.xl)
                            .padding(vertical = spacing.xs),
                ) {
                    RadioButton(
                        selected = option.value == selected,
                        onClick = null,
                    )
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = spacing.s),
                    )
                }
            }
        }
    }
}

/** Переход к ещё одному экрану: та же строка, но без текущего значения. */
@Composable
internal fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = spacing.xxl + spacing.xl)
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        SettingsLeadingIcon(icon)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.m),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Состояние PIN-кода и смена кода рядом с переключателем, без поиска по экрану. */
@Composable
internal fun SettingsPinState(
    enabled: Boolean,
    onChangePin: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m),
    ) {
        Text(
            text =
                stringResource(
                    if (enabled) R.string.settings_app_lock_state_on else R.string.settings_app_lock_state_off,
                ),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier =
                Modifier
                    .weight(1f)
                    .testTag("app_lock_state"),
        )
        if (enabled) {
            TextButton(
                onClick = onChangePin,
                modifier = Modifier.testTag("app_lock_change_pin"),
            ) {
                Text(stringResource(R.string.settings_app_lock_change_pin))
            }
        }
    }
}

/** Опасная зона: сброс отдельной красной карточкой, чтобы его не нажали случайно. */
@Composable
internal fun SettingsDangerZone(onReset: () -> Unit) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column(modifier = Modifier.padding(vertical = spacing.s)) {
            Text(
                text = stringResource(R.string.settings_danger_zone),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("reset_settings_button")
                        .clickable(role = Role.Button, onClick = onReset)
                        .heightIn(min = spacing.xxl + spacing.xl)
                        .padding(horizontal = spacing.m, vertical = spacing.xs),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_reset),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = stringResource(R.string.settings_reset_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsLeadingIcon(icon: ImageVector) {
    val spacing = LocalSpacing.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(spacing.xxl + spacing.s)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}
