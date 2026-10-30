package com.m4isper.contactsapp.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.components.AppBottomSheet
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Сторонние библиотеки и их лицензии: без сети и без сторонних экранов. */
private val LICENSES =
    listOf(
        "Kotlin, Coroutines, Serialization" to "Apache License 2.0",
        "AndroidX and Jetpack Compose" to "Apache License 2.0",
        "Metro" to "Apache License 2.0",
        "Coil" to "Apache License 2.0",
        "ZXing" to "Apache License 2.0",
    )

/** О программе: версия, лицензии и напоминание о локальном хранении. */
@Composable
internal fun SettingsAboutSection(
    version: String,
    onShowLicenses: () -> Unit,
) {
    val spacing = LocalSpacing.current
    SettingsSection(title = stringResource(R.string.settings_about)) {
        SettingsInfo(
            label = stringResource(R.string.settings_version, version),
            testTag = "app_version",
        )
        SettingsNavigationRow(
            icon = Icons.Filled.Description,
            title = stringResource(R.string.settings_licenses),
            onClick = onShowLicenses,
            testTag = "settings_licenses",
        )
        Text(
            text = stringResource(R.string.settings_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .padding(horizontal = spacing.m, vertical = spacing.xs)
                    .testTag("app_privacy_note"),
        )
    }
}

/** Лист с лицензиями: прокручивается на крупном шрифте, закрывается явно. */
@Composable
internal fun LicensesSheet(onDismiss: () -> Unit) {
    val spacing = LocalSpacing.current
    AppBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("settings_licenses_sheet"),
    ) {
        Text(
            text = stringResource(R.string.settings_licenses),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.settings_licenses_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.xs),
        )
        Column(
            modifier =
                Modifier
                    .padding(top = spacing.s)
                    .verticalScroll(rememberScrollState()),
        ) {
            LICENSES.forEach { (name, license) ->
                Column(modifier = Modifier.padding(vertical = spacing.xs)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = license,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier =
                Modifier
                    .align(Alignment.End)
                    .testTag("settings_licenses_close"),
        ) {
            Text(stringResource(R.string.action_close))
        }
    }
}

@Composable
internal fun SettingsInfo(
    label: String,
    testTag: String,
) {
    val spacing = LocalSpacing.current
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .testTag(testTag),
    )
}
