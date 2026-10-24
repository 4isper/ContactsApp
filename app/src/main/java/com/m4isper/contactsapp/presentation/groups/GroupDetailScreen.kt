package com.m4isper.contactsapp.presentation.groups

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.presentation.common.groups.GroupNameDialog
import com.m4isper.contactsapp.presentation.contacts.components.contactSubtitle
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ListAvatar
import com.m4isper.contactsapp.ui.components.LoadingState
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Группа: состав, добавление и удаление участников, переименование, слияние. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    onBack: () -> Unit,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDetailViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPicker by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf(false) }
    var showMerge by remember { mutableStateOf(false) }

    val writePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }
    LaunchedEffect(groupId) {
        viewModel.load(groupId)
        if (!hasPermission(context, Manifest.permission.WRITE_CONTACTS)) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }

    GroupDetailEvents(uiState.event, snackbarHostState, viewModel)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.title ?: stringResource(R.string.groups_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("group_detail_back_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { renameTarget = true },
                        modifier = Modifier.testTag("rename_group_detail_button"),
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.group_rename),
                        )
                    }
                    IconButton(
                        onClick = { showMerge = true },
                        enabled = uiState.groups.isNotEmpty(),
                        modifier = Modifier.testTag("merge_group_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.CallMerge,
                            contentDescription = stringResource(R.string.group_merge),
                        )
                    }
                    IconButton(
                        onClick = { deleteTarget = true },
                        modifier = Modifier.testTag("delete_group_detail_button"),
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete_group),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPicker = true },
                modifier = Modifier.testTag("add_group_member_fab"),
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = stringResource(R.string.group_add_members),
                )
            }
        },
    ) { innerPadding ->
        val screenModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        when {
            uiState.isLoading -> LoadingState(screenModifier)
            uiState.group == null ->
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Label,
                    title = stringResource(R.string.group_not_found),
                    modifier = screenModifier,
                )
            else ->
                MemberList(
                    members = uiState.members,
                    onContactClick = onContactClick,
                    onRemove = viewModel::onRemoveMember,
                    modifier = screenModifier,
                )
        }
    }

    if (showPicker) {
        val memberIds = uiState.members.map { it.contactId }.toSet()
        ContactPickerDialog(
            contacts = uiState.allContacts,
            onConfirm = { ids ->
                showPicker = false
                viewModel.onAddMembers(ids)
            },
            onDismiss = { showPicker = false },
        )
    }

    if (renameTarget) {
        GroupNameDialog(
            title = stringResource(R.string.group_rename),
            confirmLabel = stringResource(R.string.editor_save),
            initialName = uiState.group?.title.orEmpty(),
            onConfirm = { title ->
                renameTarget = false
                viewModel.onRename(title)
            },
            onDismiss = { renameTarget = false },
            confirmTestTag = "rename_group_detail_confirm",
        )
    }

    if (deleteTarget) {
        ConfirmDialog(
            title = stringResource(R.string.group_delete_title),
            text =
                stringResource(
                    R.string.group_delete_with_members,
                    uiState.members.size,
                ),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deleteTarget = false
                viewModel.onDelete()
            },
            onDismiss = { deleteTarget = false },
            danger = true,
            confirmTestTag = "confirm_group_detail_delete",
        )
    }

    if (showMerge) {
        AlertDialog(
            onDismissRequest = { showMerge = false },
            title = { Text(stringResource(R.string.group_merge)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.group_merge_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    uiState.groups.forEach { group ->
                        TextButton(
                            onClick = {
                                showMerge = false
                                viewModel.onMerge(group.id)
                            },
                            modifier = Modifier.testTag("merge_group_option_${group.id}"),
                        ) {
                            Text(group.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMerge = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            modifier = Modifier.testTag("merge_group_dialog"),
        )
    }
}

/** Участники группы: список с удалением каждого. */
@Composable
private fun MemberList(
    members: List<ContactDetails>,
    onContactClick: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    if (members.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.PersonAdd,
            title = stringResource(R.string.group_members_empty),
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = spacing.fabClearance),
        modifier = modifier.testTag("group_members_list"),
    ) {
        items(members, key = { it.contactId }) { contact ->
            MemberRow(
                contact = contact,
                onClick = { onContactClick(contact.contactId) },
                onRemove = { onRemove(contact.contactId) },
                modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            )
        }
    }
}

@Composable
private fun MemberRow(
    contact: ContactDetails,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    androidx.compose.material3.Card(
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("group_member_${contact.contactId}")
                .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            ListAvatar(
                name = contact.name,
                photoUri = contact.photo?.uri,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.m),
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contactSubtitle(contact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.testTag("remove_group_member_${contact.contactId}"),
            ) {
                Icon(
                    Icons.Filled.PersonRemove,
                    contentDescription = stringResource(R.string.group_remove_member),
                )
            }
        }
    }
}

/** Одноразовые события экрана группы: тосты и отмена удаления. */
@Composable
private fun GroupDetailEvents(
    event: GroupDetailEvent?,
    snackbarHostState: SnackbarHostState,
    viewModel: GroupDetailViewModel,
) {
    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.group_deleted)
    val undoDeleteLabel = stringResource(R.string.group_undo_delete)
    LaunchedEffect(event) {
        when (event) {
            GroupDetailEvent.MemberRemoved -> toast(context, R.string.group_member_removed)
            GroupDetailEvent.MembersAdded -> toast(context, R.string.group_members_added)
            GroupDetailEvent.GroupRenamed -> toast(context, R.string.group_renamed)
            GroupDetailEvent.GroupMerged -> toast(context, R.string.group_merged)
            GroupDetailEvent.GroupDeleted -> {
                val result =
                    snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoDeleteLabel,
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onUndoDelete()
                }
            }
            GroupDetailEvent.GroupRestored -> toast(context, R.string.group_restored)
            GroupDetailEvent.ActionFailed -> toast(context, R.string.groups_action_failed)
            null -> Unit
        }
        viewModel.onEventHandled()
    }
}

private fun toast(
    context: android.content.Context,
    resId: Int,
) {
    Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
}
