package com.m4isper.contactsapp.presentation.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsTopBar(
    title: String?,
    shareable: ContactDetails?,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title ?: stringResource(R.string.action_back),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
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
                        contentDescription =
                            stringResource(
                                if (isFavorite) {
                                    R.string.action_remove_favorite
                                } else {
                                    R.string.action_add_favorite
                                },
                            ),
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
