package com.m4isper.contactsapp.presentation.details

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.presentation.common.format.phoneText
import com.m4isper.contactsapp.ui.components.DetailsAvatar
import com.m4isper.contactsapp.ui.theme.AppMotion
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Шапка карточки: единственная тонированная поверхность на экране.
 * Имя живёт здесь, пока шапка видна; топ-бар подхватывает его после скролла.
 */
@Composable
internal fun DetailsHeader(
    details: ContactDetails,
    isFavorite: Boolean,
    onCallPrimary: () -> Unit,
    onSmsPrimary: () -> Unit,
    onEmailPrimary: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNameBottom: (Float) -> Unit,
) {
    val spacing = LocalSpacing.current
    val colors = MaterialTheme.colorScheme
    val subtitle =
        details.primaryPhone?.let { phoneText(it) }
            ?: details.organization?.company?.takeIf { it.isNotBlank() }
    val favoriteTint by
        animateColorAsState(
            targetValue = if (isFavorite) colors.tertiary else colors.onSecondaryContainer,
            animationSpec = AppMotion.effects(),
            label = "heroFavoriteTint",
        )

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .testTag("details_hero"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.m, vertical = spacing.xl),
        ) {
            DetailsAvatar(
                name = details.name,
                photoUri = details.photo?.uri,
                size = spacing.avatarHero,
            )
            Spacer(modifier = Modifier.height(spacing.m))
            Text(
                text = details.name,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSecondaryContainer,
                textAlign = TextAlign.Center,
                // Низ имени в координатах шапки: по нему топ-бар решает, когда подхватить имя.
                modifier =
                    Modifier.onGloballyPositioned { coordinates ->
                        val hero = coordinates.parentLayoutCoordinates ?: return@onGloballyPositioned
                        val top = hero.localPositionOf(coordinates, Offset.Zero).y
                        onNameBottom(top + coordinates.size.height)
                    },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSecondaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("hero_subtitle"),
                )
            }
            Spacer(modifier = Modifier.height(spacing.l))
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (details.phones.isNotEmpty()) {
                    FilledIconButton(
                        onClick = onCallPrimary,
                        modifier = Modifier.testTag("hero_call"),
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = stringResource(R.string.action_call),
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onSmsPrimary,
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surface,
                                contentColor = colors.onSurface,
                            ),
                        modifier = Modifier.testTag("hero_sms"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = stringResource(R.string.action_send_sms),
                        )
                    }
                }
                if (details.emails.isNotEmpty()) {
                    FilledTonalIconButton(
                        onClick = onEmailPrimary,
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surface,
                                contentColor = colors.onSurface,
                            ),
                        modifier = Modifier.testTag("hero_email"),
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = stringResource(R.string.action_send_email),
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_button"),
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription =
                            stringResource(
                                if (isFavorite) {
                                    R.string.action_remove_favorite
                                } else {
                                    R.string.action_add_favorite
                                },
                            ),
                        tint = favoriteTint,
                    )
                }
            }
        }
    }
}
