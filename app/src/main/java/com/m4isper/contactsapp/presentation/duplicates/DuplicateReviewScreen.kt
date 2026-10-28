package com.m4isper.contactsapp.presentation.duplicates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.format.birthdayText
import com.m4isper.contactsapp.ui.adaptive.ReadingColumn
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import com.m4isper.contactsapp.ui.theme.PillShape

/**
 * Вторая панель дубликатов: предпросмотр слияния выбранной группы.
 * На широком окне живёт рядом со списком, на компактном эту роль играет диалог.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DuplicateReviewScreen(
    targetId: Long,
    sourceIds: List<Long>,
    onBack: () -> Unit,
    viewModel: DuplicatesViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups = (uiState.content as? Loadable.Content)?.data.orEmpty()
    val group = groups.firstOrNull { group -> group.members.any { it.contactId == targetId } }
    val target = group?.members?.firstOrNull { it.contactId == targetId }
    val sources = group?.members?.filter { it.contactId in sourceIds }.orEmpty()

    // Панель может открыться первой (восстановление стека): догружаем список.
    LaunchedEffect(groups.isEmpty()) {
        if (groups.isEmpty()) viewModel.load()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.duplicates_preview_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("duplicate_review_back"),
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
        if (target == null) {
            EmptyState(
                icon = Icons.Filled.Merge,
                title = stringResource(R.string.duplicates_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            ReadingColumn(modifier = Modifier.padding(innerPadding)) {
                val spacing = LocalSpacing.current
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(spacing.m),
                ) {
                    MergePreviewContent(target = target, sources = sources)
                    Button(
                        onClick = {
                            viewModel.onMerge(targetId, sourceIds)
                            onBack()
                        },
                        enabled = !uiState.isMerging,
                        shape = PillShape,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.l)
                                .testTag("duplicate_review_merge"),
                    ) {
                        Text(stringResource(R.string.action_merge))
                    }
                }
            }
        }
    }
}

/** Поля, которые появятся у основного контакта: общий вид диалога и панели. */
@Composable
internal fun MergePreviewContent(
    target: ContactDetails,
    sources: List<ContactDetails>,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val additions = mergeAdditions(target, sources, formatBirthday = { birthdayText(it, locale) })
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.duplicates_preview_additions, target.name),
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
}

/** Подпись поля контакта в предпросмотре слияния. */
@Composable
internal fun MergeField.label(): String =
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
