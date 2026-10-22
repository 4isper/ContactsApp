package com.m4isper.contactsapp.presentation.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R

/** Основной контакт и порядок строки: звезда и стрелки под полем значения. */
@Composable
internal fun EditorRowToolbar(
    isPrimary: Boolean,
    onSetPrimary: (() -> Unit)?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    primaryTestTag: String? = null,
    moveUpTestTag: String? = null,
    moveDownTestTag: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onSetPrimary != null) {
            TextButton(
                onClick = onSetPrimary,
                modifier = primaryTestTag?.let { Modifier.testTag(it) } ?: Modifier,
            ) {
                Icon(
                    imageVector = if (isPrimary) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                )
                Text(
                    stringResource(
                        if (isPrimary) R.string.editor_primary_value else R.string.editor_make_primary,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = moveUpTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.action_move_up),
            )
        }
        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = moveDownTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.action_move_down),
            )
        }
    }
}
