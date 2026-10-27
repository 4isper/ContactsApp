package com.m4isper.contactsapp.presentation.editor.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.components.ContactAvatar
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhotoEditor(
    name: String,
    photoUri: String?,
    onPick: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactAvatar(
            name = name,
            photoUri = photoUri,
            size = spacing.avatarDetails,
            textStyle = MaterialTheme.typography.headlineMedium,
            testTag = "editor_photo",
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        // FlowRow: три подписи не влезают в 320 dp, действия переносятся, а не обрезаются.
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = onTakePhoto,
                modifier = Modifier.testTag("editor_photo_camera"),
            ) {
                Text(stringResource(R.string.editor_take_photo))
            }
            TextButton(
                onClick = onPick,
                modifier = Modifier.testTag("editor_photo_pick"),
            ) {
                Text(
                    stringResource(
                        if (photoUri == null) R.string.editor_add_photo else R.string.editor_change_photo,
                    ),
                )
            }
            if (photoUri != null) {
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("editor_photo_remove"),
                ) {
                    Text(
                        text = stringResource(R.string.editor_remove_photo),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
