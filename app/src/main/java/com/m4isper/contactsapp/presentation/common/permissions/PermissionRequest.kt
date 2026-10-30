package com.m4isper.contactsapp.presentation.common.permissions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import com.m4isper.contactsapp.ui.theme.PillShape

@Composable
internal fun PermissionRequest(
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    rationale: String? = null,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(spacing.xl),
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(spacing.xxl),
            )
            Spacer(modifier = Modifier.height(spacing.m))
            Text(
                text = rationale ?: stringResource(R.string.permission_contacts_rationale),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(spacing.xl))
            Button(
                onClick = onGrantClick,
                shape = PillShape,
            ) {
                Text(stringResource(R.string.permission_allow))
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            TextButton(onClick = onSettingsClick) {
                Text(stringResource(R.string.permission_open_settings))
            }
        }
    }
}
