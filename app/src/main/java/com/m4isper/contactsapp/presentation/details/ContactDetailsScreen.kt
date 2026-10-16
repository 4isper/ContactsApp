package com.m4isper.contactsapp.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.presentation.common.ConfirmCallDialog
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.copyNumber
import com.m4isper.contactsapp.presentation.common.openSms
import com.m4isper.contactsapp.presentation.common.rememberCallHandler
import com.m4isper.contactsapp.presentation.common.shareContact
import com.m4isper.contactsapp.ui.components.DetailsAvatar
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ErrorState
import com.m4isper.contactsapp.ui.components.LoadingState
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

private data class CallTarget(
    val name: String,
    val phone: String,
)

@Composable
fun ContactDetailsScreen(
    contactId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestCall = rememberCallHandler()

    var pendingConfirm by remember { mutableStateOf<CallTarget?>(null) }

    LaunchedEffect(contactId) {
        viewModel.load(contactId)
    }

    LifecycleResumeEffect(contactId) {
        viewModel.refresh()
        onPauseOrDispose { /* no-op */ }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DetailsTopBar(
                title =
                    (uiState.content as? Loadable.Content)?.data?.name
                        ?: stringResource(R.string.app_name),
                shareable = (uiState.content as? Loadable.Content)?.data,
                isFavorite = uiState.isFavorite,
                onBack = onBack,
                onEdit = onEdit,
                onShare = { shareContact(context, it) },
                onToggleFavorite = viewModel::toggleFavorite,
            )
        },
    ) { innerPadding ->
        val screenModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)

        when (val content = uiState.content) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Empty ->
                EmptyState(
                    icon = Icons.Filled.PersonSearch,
                    title = stringResource(R.string.contact_not_found),
                )
            is Loadable.Error ->
                ErrorState(
                    message = content.message,
                    onRetry = viewModel::refresh,
                )
            is Loadable.Content ->
                DetailsContent(
                    details = content.data,
                    onCallClick = {
                        pendingConfirm = CallTarget(content.data.name, it.number)
                    },
                    onSmsClick = { openSms(context, it.number) },
                    onCopyClick = { copyNumber(context, it.number) },
                    modifier = screenModifier,
                )
        }
    }

    pendingConfirm?.let { target ->
        ConfirmCallDialog(
            contactName = target.name,
            contactPhone = target.phone,
            onConfirm = {
                pendingConfirm = null
                requestCall(target.phone)
            },
            onDismiss = { pendingConfirm = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopBar(
    title: String?,
    shareable: ContactDetails?,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: (ContactDetails) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    TopAppBar(
        title = { Text(title ?: stringResource(R.string.action_back)) },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button"),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            if (shareable != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_button"),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_button"),
                ) {
                    Icon(
                        if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        contentDescription = stringResource(R.string.action_toggle_favorite),
                        tint =
                            if (isFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
                IconButton(onClick = { onShare(shareable) }) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_contact),
                    )
                }
            }
        },
    )
}

@Composable
private fun DetailsContent(
    details: ContactDetails,
    onCallClick: (PhoneNumber) -> Unit,
    onSmsClick: (PhoneNumber) -> Unit,
    onCopyClick: (PhoneNumber) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "header") {
            DetailsHeader(
                details = details,
                onCallPrimary = {
                    details.phones.firstOrNull()?.let(onCallClick)
                },
                onSmsPrimary = {
                    details.phones.firstOrNull()?.let(onSmsClick)
                },
            )
        }
        items(
            items = details.phones,
            key = { it.number },
        ) { phone ->
            PhoneRow(
                phone = phone,
                onClick = { onCallClick(phone) },
                onSmsClick = { onSmsClick(phone) },
                onCopyClick = { onCopyClick(phone) },
            )
        }
    }
}

@Composable
private fun DetailsHeader(
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
                        Icons.Filled.Message,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(spacing.s))
                    Text(stringResource(R.string.action_send_sms))
                }
            }
        }
    }
}

@Composable
private fun PhoneRow(
    phone: PhoneNumber,
    onClick: () -> Unit,
    onSmsClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .testTag("phone_row_${phone.number}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phone.number,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = phoneKindLabel(phone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onSmsClick) {
                Icon(
                    Icons.Filled.Message,
                    contentDescription = stringResource(R.string.action_send_sms),
                )
            }
            IconButton(onClick = onCopyClick) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy_number),
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = stringResource(R.string.action_call),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun phoneKindLabel(phone: PhoneNumber): String =
    phone.customLabel ?: stringResource(
        when (phone.kind) {
            PhoneKind.MOBILE -> R.string.phone_type_mobile
            PhoneKind.HOME -> R.string.phone_type_home
            PhoneKind.WORK -> R.string.phone_type_work
            PhoneKind.OTHER -> R.string.phone_type_other
        },
    )
