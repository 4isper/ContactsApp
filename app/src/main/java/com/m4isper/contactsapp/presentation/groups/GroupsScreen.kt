package com.m4isper.contactsapp.presentation.groups

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.account.contactAccountLabel
import com.m4isper.contactsapp.presentation.common.groups.GroupNameDialog
import com.m4isper.contactsapp.presentation.common.message
import com.m4isper.contactsapp.presentation.common.permissions.rememberWritePermissionGate
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ErrorState
import com.m4isper.contactsapp.ui.components.LoadingState
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Управление группами (метками): создание, переименование и удаление. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    onGroupClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ContactGroup?>(null) }
    var deleteTarget by remember { mutableStateOf<ContactGroup?>(null) }

    val writeGate =
        rememberWritePermissionGate(
            onDenied = {
                Toast.makeText(context, R.string.permission_write_rationale, Toast.LENGTH_SHORT).show()
            },
        )
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val errorText = uiState.error?.message()
    LaunchedEffect(uiState.error) {
        errorText?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onErrorHandled()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.groups_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("groups_back_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_group_fab"),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.groups_new),
                )
            }
        },
    ) { innerPadding ->
        val screenModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        when (val content = uiState.content) {
            is Loadable.Loading -> LoadingState(screenModifier)
            is Loadable.Empty ->
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Label,
                    title = stringResource(R.string.groups_empty),
                    modifier = screenModifier,
                )
            is Loadable.Error ->
                ErrorState(
                    error = content.error,
                    onRetry = viewModel::load,
                    modifier = screenModifier,
                )
            is Loadable.Content ->
                GroupList(
                    items = content.data,
                    onOpen = onGroupClick,
                    onRename = { renameTarget = it },
                    onDelete = { deleteTarget = it },
                    modifier = screenModifier,
                )
        }
    }

    if (showCreateDialog) {
        GroupNameDialog(
            title = stringResource(R.string.groups_new),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { title ->
                showCreateDialog = false
                viewModel.onCreate(title)
            },
            onDismiss = { showCreateDialog = false },
            confirmTestTag = "create_group_confirm",
        )
    }

    renameTarget?.let { group ->
        GroupNameDialog(
            title = stringResource(R.string.group_rename),
            confirmLabel = stringResource(R.string.editor_save),
            initialName = group.title,
            onConfirm = { title ->
                renameTarget = null
                viewModel.onRename(group.id, title)
            },
            onDismiss = { renameTarget = null },
            confirmTestTag = "rename_group_confirm",
        )
    }

    deleteTarget?.let { group ->
        ConfirmDialog(
            title = stringResource(R.string.group_delete_title),
            text = stringResource(R.string.group_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deleteTarget = null
                viewModel.onDelete(group.id)
            },
            onDismiss = { deleteTarget = null },
            danger = true,
            confirmTestTag = "confirm_group_delete",
        )
    }
}

@Composable
private fun GroupList(
    items: List<GroupItem>,
    onOpen: (Long) -> Unit,
    onRename: (ContactGroup) -> Unit,
    onDelete: (ContactGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        contentPadding = PaddingValues(bottom = spacing.fabClearance),
        modifier = modifier.testTag("groups_list"),
    ) {
        items(
            items = items,
            key = { it.group.id },
        ) { item ->
            GroupRow(
                item = item,
                onOpen = { onOpen(item.group.id) },
                onRename = { onRename(item.group) },
                onDelete = { onDelete(item.group) },
                modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            )
        }
    }
}

@Composable
private fun GroupRow(
    item: GroupItem,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
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
                .testTag("group_row_${item.group.id}")
                .clickable(onClick = onOpen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(
                    start = spacing.m,
                    top = spacing.s,
                    bottom = spacing.s,
                ),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.group.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.group_members_count,
                            item.contactCount,
                            item.contactCount,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = contactAccountLabel(item.group.account),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("group_account_${item.group.id}"),
                )
            }
            IconButton(
                onClick = onRename,
                modifier = Modifier.testTag("rename_group_${item.group.id}"),
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.group_rename),
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_group_${item.group.id}"),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete_group),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
