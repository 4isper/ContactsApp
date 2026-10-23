package com.m4isper.contactsapp.presentation.editor.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun SectionHeader(
    titleRes: Int,
    onAdd: (() -> Unit)? = null,
    addLabelRes: Int? = null,
    addTestTag: String? = null,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m)
                .padding(top = spacing.m, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (onAdd != null) {
            IconButton(
                onClick = onAdd,
                modifier = if (addTestTag != null) Modifier.testTag(addTestTag) else Modifier,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = addLabelRes?.let { stringResource(it) },
                )
            }
        }
    }
}
