package com.m4isper.contactsapp.presentation.editor.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.m4isper.contactsapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopBar(
    title: String,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    showSave: Boolean = true,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("editor_back_button"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            if (showSave) {
                TextButton(
                    onClick = onSave,
                    enabled = saveEnabled,
                    modifier = Modifier.testTag("editor_save_button"),
                ) {
                    Text(stringResource(R.string.editor_save))
                }
            }
        },
    )
}
