package com.m4isper.contactsapp.presentation.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Строка поля: ведущая иконка, значение и действия; поверхность задаёт карточка секции. */
@Composable
internal fun DetailsFieldRow(
    icon: ImageVector,
    primary: String,
    secondary: String?,
    actions: List<RowAction>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            onClickLabel = stringResource(R.string.action_call),
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ).padding(horizontal = spacing.m, vertical = spacing.m),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(spacing.m))
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

/** Секция полей: одна скруглённая карточка на секцию, в ритме строк списка. */
@Composable
internal fun DetailsSectionCard(section: DetailsSection) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column {
            section.fields.forEach { field ->
                DetailsFieldRow(
                    icon = field.icon,
                    primary = field.primary,
                    secondary = field.secondary,
                    actions = field.actions,
                    onClick = field.onClick,
                    modifier = field.tag?.let { Modifier.testTag(it) } ?: Modifier,
                )
            }
        }
    }
}

/** Заголовок секции полей: подпись, а не поверхность. */
@Composable
internal fun DetailsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = spacing.m, end = spacing.m, top = spacing.l, bottom = spacing.s),
    )
}

private val ICON_SIZE = 24.dp
