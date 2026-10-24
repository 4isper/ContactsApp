package com.m4isper.contactsapp.presentation.duplicates

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.duplicates.DuplicateGroup
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.contacts.components.contactSubtitle
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ErrorState
import com.m4isper.contactsapp.ui.components.ListAvatar
import com.m4isper.contactsapp.ui.components.LoadingState
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Поиск дубликатов: группы похожих контактов и слияние в выбранный основной. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DuplicatesViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMerge by remember { mutableStateOf<PendingMerge?>(null) }

    val writePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }
    LaunchedEffect(Unit) {
        if (!hasPermission(context, Manifest.permission.WRITE_CONTACTS)) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }
    val mergedMessage = stringResource(R.string.duplicates_merged)
    val undoLabel = stringResource(R.string.duplicates_undo)

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    DuplicatesScreenEvents(
        event = uiState.event,
        snackbarHostState = snackbarHostState,
        context = context,
        mergedMessage = mergedMessage,
        undoLabel = undoLabel,
        viewModel = viewModel,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.duplicates_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("duplicates_back_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
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
                    icon = Icons.Filled.Merge,
                    title = stringResource(R.string.duplicates_empty),
                    modifier = screenModifier,
                )
            is Loadable.Error ->
                ErrorState(
                    error = content.error,
                    onRetry = viewModel::load,
                    modifier = screenModifier,
                )
            is Loadable.Content ->
                DuplicateGroupList(
                    groups = content.data,
                    merging = uiState.isMerging,
                    onMergePreview = { target, sources -> pendingMerge = PendingMerge(target, sources) },
                    modifier = screenModifier,
                )
        }
    }

    // Предпросмотр слияния: показываем, что добавится к основному контакту.
    pendingMerge?.let { pending ->
        MergePreviewDialog(
            pending = pending,
            onConfirm = {
                pendingMerge = null
                viewModel.onMerge(
                    pending.target.contactId,
                    pending.sources.map { it.contactId },
                )
            },
            onDismiss = { pendingMerge = null },
        )
    }
}

/** Обрабатывает события экрана дубликатов: показывает Snackbar/Toast после слияния или ошибки. */
@Composable
private fun DuplicatesScreenEvents(
    event: DuplicatesEvent?,
    snackbarHostState: SnackbarHostState,
    context: Context,
    mergedMessage: String,
    undoLabel: String,
    viewModel: DuplicatesViewModel,
) {
    LaunchedEffect(event) {
        when (event) {
            DuplicatesEvent.Merged -> {
                val result =
                    snackbarHostState.showSnackbar(
                        message = mergedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onUndoMerge()
                }
            }
            DuplicatesEvent.Unmerged -> Toast.makeText(context, R.string.duplicates_unmerged, Toast.LENGTH_SHORT).show()
            DuplicatesEvent.MergeFailed ->
                Toast.makeText(context, R.string.duplicates_action_failed, Toast.LENGTH_SHORT).show()
            DuplicatesEvent.UnmergeFailed ->
                Toast.makeText(context, R.string.duplicates_action_failed, Toast.LENGTH_SHORT).show()
            null -> Unit
        }
        viewModel.onEventHandled()
    }
}

/** Диалог предпросмотра: поля, которые появятся у основного контакта. */
@Composable
private fun MergePreviewDialog(
    pending: PendingMerge,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val additions = mergeAdditions(pending.target, pending.sources)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.duplicates_preview_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.duplicates_preview_additions, pending.target.name),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (additions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.duplicates_preview_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    additions.forEach { addition ->
                        Text(
                            text = addition.field.label() + ": " + addition.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("merge_preview_confirm"),
            ) {
                Text(stringResource(R.string.action_merge))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag("merge_preview_dialog"),
    )
}

/** Подпись поля контакта в предпросмотре слияния. */
@Composable
private fun MergeField.label(): String =
    when (this) {
        MergeField.NAME -> stringResource(R.string.details_field_name)
        MergeField.PHONE -> stringResource(R.string.details_field_phone)
        MergeField.EMAIL -> stringResource(R.string.details_field_email)
        MergeField.ADDRESS -> stringResource(R.string.details_field_address)
        MergeField.WEBSITE -> stringResource(R.string.details_field_website)
        MergeField.ORGANIZATION -> stringResource(R.string.details_field_organization)
        MergeField.NOTE -> stringResource(R.string.details_field_note)
        MergeField.BIRTHDAY -> stringResource(R.string.details_field_birthday)
    }

/** Слияние, ожидающее подтверждения: основной контакт и источники. */
private data class PendingMerge(
    val target: ContactDetails,
    val sources: List<ContactDetails>,
)

@Composable
private fun DuplicateGroupList(
    groups: List<DuplicateGroup>,
    merging: Boolean,
    onMergePreview: (ContactDetails, List<ContactDetails>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier.testTag("duplicates_list"),
    ) {
        items(
            items = groups,
            key = { group -> group.members.minOf { it.contactId } },
        ) { group ->
            DuplicateGroupCard(
                group = group,
                merging = merging,
                onMergePreview = onMergePreview,
                modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    merging: Boolean,
    onMergePreview: (ContactDetails, List<ContactDetails>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val defaultPrimaryId = group.members.first().contactId
    var selectedId by rememberSaveable { mutableStateOf(defaultPrimaryId) }
    // Если контакт-основной исчез после слияния, возвращаемся к самому заполненному.
    val primaryId =
        selectedId.takeIf { id -> group.members.any { it.contactId == id } } ?: defaultPrimaryId
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("duplicate_group_${group.members.minOf { it.contactId }}"),
    ) {
        Column(modifier = Modifier.padding(vertical = spacing.s)) {
            if (group.nameOnly) {
                Text(
                    text = stringResource(R.string.duplicates_name_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
                )
            }
            Text(
                text = stringResource(R.string.duplicates_primary_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.m),
            )
            Column(modifier = Modifier.selectableGroup()) {
                group.members.forEach { member ->
                    DuplicateMemberRow(
                        member = member,
                        selected = member.contactId == primaryId,
                        onSelect = { selectedId = member.contactId },
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m, vertical = spacing.xs),
            ) {
                FilledTonalButton(
                    onClick = {
                        val target = group.members.first { it.contactId == primaryId }
                        val sources = group.members.filter { it.contactId != primaryId }
                        onMergePreview(target, sources)
                    },
                    enabled = !merging,
                    modifier = Modifier.testTag("merge_group_${group.members.minOf { it.contactId }}"),
                ) {
                    Text(stringResource(R.string.action_merge))
                }
            }
        }
    }
}

@Composable
private fun DuplicateMemberRow(
    member: ContactDetails,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("duplicate_member_${member.contactId}")
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(spacing.m))
        ListAvatar(
            name = member.name,
            photoUri = member.photo?.uri,
        )
        Spacer(modifier = Modifier.width(spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = contactSubtitle(member),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
