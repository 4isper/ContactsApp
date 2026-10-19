package com.m4isper.contactsapp.presentation.duplicates

import android.widget.Toast
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.contacts.contactSubtitle
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

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.event) {
        val messageRes =
            when (uiState.event) {
                DuplicatesEvent.Merged -> R.string.duplicates_merged
                DuplicatesEvent.MergeFailed -> R.string.duplicates_action_failed
                null -> return@LaunchedEffect
            }
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
        viewModel.onEventHandled()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                    message = content.message,
                    onRetry = viewModel::load,
                    modifier = screenModifier,
                )
            is Loadable.Content ->
                DuplicateGroupList(
                    groups = content.data,
                    merging = uiState.isMerging,
                    onMerge = viewModel::onMerge,
                    modifier = screenModifier,
                )
        }
    }
}

@Composable
private fun DuplicateGroupList(
    groups: List<DuplicateGroup>,
    merging: Boolean,
    onMerge: (Long, List<Long>) -> Unit,
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
                onMerge = onMerge,
                modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs),
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    merging: Boolean,
    onMerge: (Long, List<Long>) -> Unit,
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
                        onMerge(primaryId, group.members.map { it.contactId }.filterNot { it == primaryId })
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
            photoUri = member.photoUri,
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
