package com.m4isper.contactsapp.presentation.editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R

/** Действия строки за одним меню: основное значение, порядок и удаление. */
@Composable
internal fun FieldRowMenu(
    prefix: String,
    rowId: Long,
    isPrimary: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    canRemove: Boolean = true,
    deleteLabel: String,
    onSetPrimary: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .size(40.dp)
                    .testTag("editor_${prefix}_more_$rowId"),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.editor_row_actions),
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (onSetPrimary != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (isPrimary) R.string.editor_primary_value else R.string.editor_make_primary,
                            ),
                        )
                    },
                    onClick = {
                        onSetPrimary()
                        expanded = false
                    },
                    enabled = !isPrimary,
                    leadingIcon = {
                        if (isPrimary) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.testTag("editor_${prefix}_primary_$rowId"),
                )
                HorizontalDivider()
            }
            if (onMoveUp != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move_up)) },
                    onClick = {
                        onMoveUp()
                        expanded = false
                    },
                    enabled = canMoveUp,
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
                    modifier = Modifier.testTag("editor_${prefix}_up_$rowId"),
                )
            }
            if (onMoveDown != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_move_down)) },
                    onClick = {
                        onMoveDown()
                        expanded = false
                    },
                    enabled = canMoveDown,
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                    modifier = Modifier.testTag("editor_${prefix}_down_$rowId"),
                )
            }
            if (onRemove != null && canRemove) {
                if (onMoveUp != null || onMoveDown != null) HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(deleteLabel) },
                    onClick = {
                        onRemove()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier.testTag("editor_${prefix}_delete_$rowId"),
                )
            }
        }
    }
}

/** Удаление пустой необязательной строки остаётся на виду, без раскрытия меню. */
@Composable
internal fun DeleteFieldButton(
    testTag: String,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(40.dp)
                .testTag(testTag),
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
    }
}
