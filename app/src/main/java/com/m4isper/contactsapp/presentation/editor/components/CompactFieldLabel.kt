package com.m4isper.contactsapp.presentation.editor.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow

/** Ярлык компактного поля: одна строка даже в узком окне, иначе поле вырастает вдвое. */
@Composable
internal fun CompactFieldLabel(resId: Int) {
    Text(
        text = stringResource(resId),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
