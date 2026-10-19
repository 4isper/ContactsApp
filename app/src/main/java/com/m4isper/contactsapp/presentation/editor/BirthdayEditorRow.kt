package com.m4isper.contactsapp.presentation.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.presentation.common.format.birthdayText
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import java.time.LocalDate

@Composable
internal fun BirthdayEditorRow(
    birthday: EditableBirthday,
    onPickClick: () -> Unit,
    onBirthdayChange: (Birthday?) -> Unit,
) {
    val spacing = LocalSpacing.current
    val value = birthday.value
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onPickClick,
                modifier = Modifier.testTag("editor_birthday_button"),
            ) {
                Text(
                    text =
                        if (value == null) {
                            stringResource(R.string.editor_birthday_add)
                        } else {
                            birthdayText(value)
                        },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (value != null) {
                IconButton(
                    onClick = { onBirthdayChange(null) },
                    modifier = Modifier.testTag("editor_birthday_remove"),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.editor_birthday_remove),
                    )
                }
            }
        }
        if (value != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = value.year == null,
                            role = Role.Switch,
                            onValueChange = { noYear ->
                                onBirthdayChange(
                                    value.copy(year = if (noYear) null else LocalDate.now().year),
                                )
                            },
                        ),
            ) {
                Text(
                    text = stringResource(R.string.editor_birthday_no_year),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = value.year == null,
                    onCheckedChange = null,
                    modifier = Modifier.testTag("editor_birthday_switch"),
                )
            }
        }
    }
}
