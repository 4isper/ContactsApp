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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.presentation.common.ConfirmCallDialog
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.addressKindLabel
import com.m4isper.contactsapp.presentation.common.birthdayText
import com.m4isper.contactsapp.presentation.common.contactEventLabel
import com.m4isper.contactsapp.presentation.common.copyToClipboard
import com.m4isper.contactsapp.presentation.common.emailKindLabel
import com.m4isper.contactsapp.presentation.common.instantMessageLabel
import com.m4isper.contactsapp.presentation.common.openEmail
import com.m4isper.contactsapp.presentation.common.openMap
import com.m4isper.contactsapp.presentation.common.openSms
import com.m4isper.contactsapp.presentation.common.openWebsite
import com.m4isper.contactsapp.presentation.common.phoneKindLabel
import com.m4isper.contactsapp.presentation.common.relationLabel
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
    val phone: PhoneNumber,
)

private data class RowAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
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
                onShare = { viewModel.vCardText()?.let { content -> shareContact(context, content) } },
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
                    onCallClick = { phone ->
                        pendingConfirm = CallTarget(content.data.name, phone)
                    },
                    onSmsClick = { openSms(context, it.number) },
                    onCopyClick = { copyToClipboard(context, it) },
                    modifier = screenModifier,
                )
        }
    }

    pendingConfirm?.let { target ->
        ConfirmCallDialog(
            contactName = target.name,
            phoneText = phoneText(target.phone),
            onConfirm = {
                pendingConfirm = null
                requestCall(target.phone.number)
            },
            onDismiss = { pendingConfirm = null },
        )
    }
}

@Composable
private fun phoneText(phone: PhoneNumber): String =
    stringResource(
        R.string.phone_with_kind,
        phone.number,
        phone.customLabel ?: phoneKindLabel(phone.kind),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopBar(
    title: String?,
    shareable: ContactDetails?,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
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
                IconButton(onClick = onShare) {
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
    onCopyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("details_list"),
    ) {
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
        details.nickname?.let { nickname ->
            item(key = "nickname") {
                DataRow(
                    primary = nickname,
                    secondary = null,
                    actions =
                        listOf(
                            RowAction(
                                Icons.Filled.ContentCopy,
                                stringResource(R.string.action_copy),
                            ) { onCopyClick(nickname) },
                        ),
                    modifier = Modifier.testTag("nickname_row"),
                )
            }
        }
        items(
            items = details.phones,
            key = { "phone_${it.number}" },
        ) { phone ->
            PhoneRow(
                phone = phone,
                onClick = { onCallClick(phone) },
                onSmsClick = { onSmsClick(phone) },
                onCopyClick = { onCopyClick(phone.number) },
            )
        }
        items(
            items = details.emails,
            key = { "email_${it.address}" },
        ) { email ->
            DataRow(
                primary = email.address,
                secondary = email.customLabel ?: emailKindLabel(email.kind),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.Email,
                            stringResource(R.string.action_send_email),
                        ) { openEmail(context, email.address) },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(email.address) },
                    ),
                modifier = Modifier.testTag("email_row_${email.address}"),
            )
        }
        items(
            items = details.instantMessages,
            key = { "im_${it.address}" },
        ) { im ->
            DataRow(
                primary = im.address,
                secondary = instantMessageLabel(im),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(im.address) },
                    ),
                modifier = Modifier.testTag("im_row_${im.address}"),
            )
        }
        items(
            items = details.addresses,
            key = { "address_${it.address}" },
        ) { address ->
            DataRow(
                primary = address.address,
                secondary = address.customLabel ?: addressKindLabel(address.kind),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.LocationOn,
                            stringResource(R.string.action_open_map),
                        ) { openMap(context, address.address) },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(address.address) },
                    ),
                modifier = Modifier.testTag("address_row_${address.address}"),
            )
        }
        items(
            items = details.websites,
            key = { "website_${it.url}" },
        ) { website ->
            DataRow(
                primary = website.url,
                secondary = null,
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.Public,
                            stringResource(R.string.action_open_website),
                        ) { openWebsite(context, website.url) },
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(website.url) },
                    ),
                modifier = Modifier.testTag("website_row_${website.url}"),
            )
        }
        details.organization?.let { organization ->
            item(key = "organization") {
                DataRow(
                    primary = organization.company.ifBlank { organization.title },
                    secondary = organization.title.takeIf { organization.company.isNotBlank() },
                    actions = emptyList(),
                    modifier = Modifier.testTag("organization_row"),
                )
            }
        }
        details.birthday?.let { birthday ->
            item(key = "birthday") {
                DataRow(
                    primary = birthdayText(birthday),
                    secondary = null,
                    actions = emptyList(),
                    modifier = Modifier.testTag("birthday_row"),
                )
            }
        }
        items(
            items = details.events,
            key = { "event_${it.date.toStorageString()}_${it.kind.name}" },
        ) { event ->
            DataRow(
                primary = birthdayText(event.date),
                secondary = contactEventLabel(event),
                actions = emptyList(),
                modifier = Modifier.testTag("event_row_${event.date.toStorageString()}"),
            )
        }
        items(
            items = details.relations,
            key = { "relation_${it.name}" },
        ) { relation ->
            DataRow(
                primary = relation.name,
                secondary = relationLabel(relation),
                actions =
                    listOf(
                        RowAction(
                            Icons.Filled.ContentCopy,
                            stringResource(R.string.action_copy),
                        ) { onCopyClick(relation.name) },
                    ),
                modifier = Modifier.testTag("relation_row_${relation.name}"),
            )
        }
        details.note?.let { note ->
            item(key = "note") {
                DataRow(
                    primary = note,
                    secondary = null,
                    actions = emptyList(),
                    modifier = Modifier.testTag("note_row"),
                )
            }
        }
    }
}

@Composable
private fun DataRow(
    primary: String,
    secondary: String?,
    actions: List<RowAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primary,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (secondary != null) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Icon(
                        action.icon,
                        contentDescription = action.contentDescription,
                    )
                }
            }
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
                    text = phone.customLabel ?: phoneKindLabel(phone.kind),
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
                    contentDescription = stringResource(R.string.action_copy),
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
