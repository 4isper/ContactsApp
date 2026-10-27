package com.m4isper.contactsapp.presentation.editor.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EventKind
import com.m4isper.contactsapp.presentation.common.format.birthdayText
import com.m4isper.contactsapp.presentation.common.format.eventKindLabel
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.components.KindMenu
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun EventEditorRow(
    row: EditorRow<EditableEvent>,
    error: String? = null,
    onKindChange: (Long, EventKind) -> Unit,
    onDatePickClick: (Long) -> Unit,
    onDateChange: (Long, Birthday?) -> Unit,
    focusRequester: FocusRequester? = null,
    onAddYearClick: (Long) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val date = row.value.date
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
        Column(modifier = Modifier.padding(spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindMenu(
                    current = row.value.kind,
                    options = EventKind.entries,
                    label = { eventKindLabel(it) },
                    onSelect = { onKindChange(row.rowId, it) },
                )
                TextButton(
                    onClick = { onDatePickClick(row.rowId) },
                    modifier =
                        (focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                            .testTag("editor_event_date_button")
                            .weight(1f, fill = false),
                ) {
                    Text(
                        text =
                            if (date == null) {
                                stringResource(R.string.editor_event_date_add)
                            } else {
                                birthdayText(date)
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(row.rowId) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete_event),
                    )
                }
            }
            if (error != null) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (date != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // Тег на toggleable-строке: клик обрабатывает строка, не Switch.
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("editor_event_switch")
                            .toggleable(
                                value = date.year == null,
                                role = Role.Switch,
                                // Как у дня рождения: выключение switch открывает
                                // пикер для явного выбора года.
                                onValueChange = { noYear ->
                                    if (noYear) {
                                        onDateChange(row.rowId, date.copy(year = null))
                                    } else {
                                        onAddYearClick(row.rowId)
                                    }
                                },
                            ),
                ) {
                    Text(
                        text = stringResource(R.string.editor_birthday_no_year),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = date.year == null,
                        onCheckedChange = null,
                    )
                }
            }
            if (row.value.kind == EventKind.OTHER) {
                Spacer(modifier = Modifier.height(spacing.s))
                OutlinedTextField(
                    value = row.value.customLabel.orEmpty(),
                    onValueChange = { onCustomLabelChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_kind_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
