package com.m4isper.contactsapp.presentation.import

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ImportDecision
import com.m4isper.contactsapp.domain.model.ImportFailureReason
import com.m4isper.contactsapp.domain.model.ImportOutcome
import com.m4isper.contactsapp.domain.vcard.VCardInvalidContact
import com.m4isper.contactsapp.domain.vcard.VCardIssue
import com.m4isper.contactsapp.presentation.editor.AccountPickerDialog
import com.m4isper.contactsapp.presentation.editor.AccountRow
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.LoadingState
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Предпросмотр импорта .vcf: что в файле, какие контакты уже есть и что будет сделано.
 * Решение по каждому контакту принимается до записи в провайдер.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportPreviewViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAccountPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (uiState.outcome == null) {
                        TextButton(
                            onClick = viewModel::onImport,
                            enabled = uiState.canImport,
                            modifier = Modifier.testTag("import_confirm_button"),
                        ) {
                            Text(stringResource(R.string.import_action_import))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isPreparing -> ImportLoading(Modifier.padding(innerPadding))
            uiState.failed ->
                EmptyState(
                    icon = Icons.Filled.PersonSearch,
                    title = stringResource(R.string.import_parse_failed),
                    modifier = Modifier.padding(innerPadding),
                )
            uiState.entries.isEmpty() && uiState.invalid.isEmpty() ->
                EmptyState(
                    icon = Icons.Filled.PersonSearch,
                    title = stringResource(R.string.import_empty),
                    modifier = Modifier.padding(innerPadding),
                )
            else ->
                ImportPreviewList(
                    uiState = uiState,
                    onDecisionChange = viewModel::onDecisionChange,
                    onAccountClick = { showAccountPicker = true },
                    onDone = onDone,
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }

    if (showAccountPicker) {
        AccountPickerDialog(
            accounts = uiState.accounts,
            selected = uiState.selectedAccount,
            onSelect = {
                viewModel.onAccountChange(it)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }
}

@Composable
private fun ImportLoading(modifier: Modifier = Modifier) {
    LoadingState(modifier)
}

@Composable
private fun ImportPreviewList(
    uiState: ImportPreviewUiState,
    onDecisionChange: (Int, ImportDecision) -> Unit,
    onAccountClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        item(key = "summary") {
            ImportSummary(uiState)
        }
        if (uiState.accounts.size > 1) {
            item(key = "account") {
                AccountRow(
                    account = uiState.selectedAccount,
                    onClick = onAccountClick,
                )
            }
        }
        items(uiState.entries, key = { it.index }) { entry ->
            ImportEntryCard(
                entry = entry,
                onDecisionChange = { decision -> onDecisionChange(entry.index, decision) },
            )
        }
        if (uiState.invalid.isNotEmpty()) {
            item(key = "invalid") {
                ImportInvalidCard(uiState.invalid)
            }
        }
        uiState.outcome?.let { outcome ->
            item(key = "result") {
                ImportResultCard(outcome, onDone)
            }
        }
    }
}

/** Итог файла: сколько контактов, сколько дублей и сколько блоков пропущено. */
@Composable
private fun ImportSummary(uiState: ImportPreviewUiState) {
    val spacing = LocalSpacing.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            Text(
                text = stringResource(R.string.import_preview_total, uiState.entries.size),
                style = MaterialTheme.typography.titleMedium,
            )
            if (uiState.duplicateCount > 0) {
                Text(
                    text = stringResource(R.string.import_preview_duplicates, uiState.duplicateCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (uiState.invalid.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.import_preview_invalid, uiState.invalid.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Карточка контакта из файла: имя, детали и решение — создать, влить или пропустить. */
@Composable
private fun ImportEntryCard(
    entry: ImportPreviewEntry,
    onDecisionChange: (ImportDecision) -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("import_entry_${entry.index}"),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            Text(
                text = entry.name.ifBlank { stringResource(R.string.import_entry_unnamed) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.summary.isNotBlank()) {
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.duplicates.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.import_entry_duplicate, entry.duplicates.first().name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.hasPhoto) {
                Text(
                    text = stringResource(R.string.import_entry_photo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.categories.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.import_entry_categories, entry.categories.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                modifier = Modifier.padding(top = spacing.s),
            ) {
                FilterChip(
                    selected = entry.decision is ImportDecision.Create,
                    onClick = { onDecisionChange(ImportDecision.Create) },
                    label = { Text(stringResource(R.string.import_decision_create)) },
                    modifier = Modifier.testTag("import_decision_create_${entry.index}"),
                )
                FilterChip(
                    selected = entry.decision is ImportDecision.Merge,
                    onClick = {
                        val target = entry.duplicates.firstOrNull()?.contactId
                        if (target != null) onDecisionChange(ImportDecision.Merge(target))
                    },
                    enabled = entry.duplicates.isNotEmpty(),
                    label = { Text(stringResource(R.string.import_decision_merge)) },
                    modifier = Modifier.testTag("import_decision_merge_${entry.index}"),
                )
                FilterChip(
                    selected = entry.decision is ImportDecision.Skip,
                    onClick = { onDecisionChange(ImportDecision.Skip) },
                    label = { Text(stringResource(R.string.import_decision_skip)) },
                    modifier = Modifier.testTag("import_decision_skip_${entry.index}"),
                )
            }
        }
    }
}

/** Пропущенные блоки файла: в них не было ни одного поддерживаемого поля. */
@Composable
private fun ImportInvalidCard(invalid: List<VCardInvalidContact>) {
    val spacing = LocalSpacing.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            Text(
                text = stringResource(R.string.import_invalid_title),
                style = MaterialTheme.typography.titleMedium,
            )
            invalid.forEach { entry ->
                Text(
                    text =
                        entry.name.ifBlank { stringResource(R.string.import_entry_unnamed) } +
                            " — " +
                            stringResource(
                                when (entry.issue) {
                                    VCardIssue.EMPTY -> R.string.import_invalid_empty
                                    VCardIssue.UNSUPPORTED -> R.string.import_invalid_unsupported
                                },
                            ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Итог импорта: сколько создано, слито, пропущено и что не получилось. */
@Composable
private fun ImportResultCard(
    outcome: ImportOutcome,
    onDone: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("import_result_card"),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            Text(
                text = stringResource(R.string.import_result_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.import_result_created, outcome.created),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.import_result_merged, outcome.merged),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.import_result_skipped, outcome.skipped),
                style = MaterialTheme.typography.bodyMedium,
            )
            outcome.failures.forEach { failure ->
                Text(
                    text =
                        failure.name.ifBlank { stringResource(R.string.import_entry_unnamed) } +
                            " — " +
                            stringResource(failure.reason.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (outcome.skippedCategories.isNotEmpty()) {
                Text(
                    text =
                        stringResource(
                            R.string.import_result_categories,
                            outcome.skippedCategories.joinToString(", "),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onDone,
                modifier = Modifier.testTag("import_done_button"),
            ) {
                Text(stringResource(R.string.import_action_done))
            }
        }
    }
}

private fun ImportFailureReason.labelRes(): Int =
    when (this) {
        ImportFailureReason.INVALID -> R.string.import_failure_invalid
        ImportFailureReason.FAILED -> R.string.import_failure_failed
        ImportFailureReason.MERGE_FAILED -> R.string.import_failure_merge
        ImportFailureReason.PHOTO_FAILED -> R.string.import_failure_photo
        ImportFailureReason.GROUP_FAILED -> R.string.import_failure_group
    }
