package com.m4isper.contactsapp.presentation.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Строка поля: плоская, с ведущей иконкой и действиями; разделитель живёт внутри строки. */
@Composable
internal fun DetailsFieldRow(
    icon: ImageVector,
    primary: String,
    secondary: String?,
    actions: List<RowAction>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
) {
    val spacing = LocalSpacing.current
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
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
        if (showDivider) {
            HorizontalDivider(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = spacing.m + ICON_SIZE + spacing.m),
            )
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
