package com.m4isper.contactsapp.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.ui.components.DetailsAvatar
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun DetailsHeader(
    details: ContactDetails,
    onCallPrimary: () -> Unit,
    onSmsPrimary: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m)
                .padding(top = spacing.m),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xl),
        ) {
            DetailsAvatar(
                name = details.name,
                photoUri = details.photoUri,
            )
            Spacer(modifier = Modifier.height(spacing.m))
            Text(
                text = details.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            if (details.phones.isNotEmpty()) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.phone_count,
                            details.phoneCount,
                            details.phoneCount,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (details.emails.isNotEmpty()) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.email_count,
                            details.emailCount,
                            details.emailCount,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (details.phones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.m))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                ) {
                    FilledTonalButton(onClick = onCallPrimary) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.s))
                        Text(stringResource(R.string.action_call))
                    }
                    OutlinedButton(onClick = onSmsPrimary) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(spacing.s))
                        Text(stringResource(R.string.action_send_sms))
                    }
                }
            }
        }
    }
}
