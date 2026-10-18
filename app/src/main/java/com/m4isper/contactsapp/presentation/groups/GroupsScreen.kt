package com.m4isper.contactsapp.presentation.groups

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.groups.GroupNameDialog
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
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ContactGroup?>(null) }
    var deleteTarget by remember { mutableStateOf<ContactGroup?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
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
                    icon = Icons.Filled.Label,
                    title = stringResource(R.string.groups_empty),
                    modifier = screenModifier,
                )
            is Loadable.Error ->
                ErrorState(
                    message = content.message,
                    onRetry = viewModel::load,
                    modifier = screenModifier,
                )
            is Loadable.Content ->
                GroupList(
                    items = content.data,
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
    onRename: (ContactGroup) -> Unit,
    onDelete: (ContactGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier.testTag("groups_list"),
    ) {
        items(
            items = items,
            key = { it.group.id },
        ) { item ->
            GroupRow(
                item = item,
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
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("group_row_${item.group.id}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = LocalSpacing.current.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.group.title,
                    style = MaterialTheme.typography.titleMedium,
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
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
        }
    }
}
