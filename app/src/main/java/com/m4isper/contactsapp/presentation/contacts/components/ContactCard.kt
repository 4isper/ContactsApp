package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.presentation.common.share.copyToClipboard
import com.m4isper.contactsapp.presentation.common.share.openEmail
import com.m4isper.contactsapp.ui.components.HapticImpact
import com.m4isper.contactsapp.ui.components.ListAvatar
import com.m4isper.contactsapp.ui.components.performHapticFeedback
import com.m4isper.contactsapp.ui.theme.BrandIcon
import com.m4isper.contactsapp.ui.theme.BrandIcons
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

private val QuickActionSize = 40.dp

@Composable
internal fun ContactCard(
    contact: ContactDetails,
    isFavorite: Boolean,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    density: ListDensity = ListDensity.COMFORTABLE,
    onCall: (() -> Unit)? = null,
    onSms: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    showFavoriteQuickAction: Boolean = true,
) {
    val ctx = LocalContext.current
    val spacing = LocalSpacing.current
    val call = onCall?.takeIf { contact.phones.isNotEmpty() }
    val sms = onSms?.takeIf { contact.phones.isNotEmpty() }

    if (isSelectionMode || (call == null && sms == null)) {
        ContactCardSurface(
            contact = contact,
            isFavorite = isFavorite,
            selected = selected,
            isSelectionMode = isSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            density = density,
            onCall = call,
            onSms = sms,
            onToggleFavorite = onToggleFavorite,
            showFavoriteQuickAction = showFavoriteQuickAction,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.m, vertical = spacing.xs),
        )
        return
    }

    val swipeState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val currentCall by rememberUpdatedState(call)
    val currentSms by rememberUpdatedState(sms)
    val onSwipeDismiss =
        remember(swipeState, scope) {
            { direction: SwipeToDismissBoxValue ->
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        performHapticFeedback(ctx, HapticImpact.Light)
                        currentSms?.invoke()
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        performHapticFeedback(ctx, HapticImpact.Medium)
                        currentCall?.invoke()
                    }
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                scope.launch { swipeState.reset() }
                Unit
            }
        }
    SwipeToDismissBox(
        state = swipeState,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
        enableDismissFromStartToEnd = sms != null,
        enableDismissFromEndToStart = call != null,
        onDismiss = onSwipeDismiss,
        backgroundContent = {
            SwipeActionBackground(
                direction = swipeState.dismissDirection,
                modifier = Modifier.fillMaxSize(),
            )
        },
    ) {
        ContactCardSurface(
            contact = contact,
            isFavorite = isFavorite,
            selected = selected,
            isSelectionMode = isSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            density = density,
            onCall = call,
            onSms = sms,
            onToggleFavorite = onToggleFavorite,
            showFavoriteQuickAction = showFavoriteQuickAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("LongMethod") // Осознанно: карточка рисует аватар, имя, метки и действия в одной composable-функции.
@Composable
private fun ContactCardSurface(
    contact: ContactDetails,
    isFavorite: Boolean,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    density: ListDensity,
    onCall: (() -> Unit)?,
    onSms: (() -> Unit)?,
    onToggleFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showFavoriteQuickAction: Boolean = true,
) {
    val spacing = LocalSpacing.current
    val ctx = LocalContext.current
    val compact = density == ListDensity.COMPACT
    // Меню показываем, когда есть хоть одно действие: телефон (звонок/СМС/копия)
    // или почта (письмо/копия) — email-only контакты тоже получают быстрые действия.
    val hasQuickActions = onCall != null || onSms != null || contact.primaryEmail != null

    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    val subtitleColor =
        if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border =
            if (selected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .testTag("contact_row_${contact.contactId}")
                .then(
                    if (isSelectionMode) {
                        Modifier.selectable(
                            selected = selected,
                            role = Role.Checkbox,
                            onClick = onClick,
                        )
                    } else {
                        Modifier.combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                    },
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(
                    horizontal = spacing.m,
                    vertical = if (compact) spacing.xs else spacing.m,
                ),
        ) {
            ListAvatar(
                name = contact.name,
                photoUri = contact.photo?.uri,
                size = if (compact) spacing.avatarList - spacing.s else spacing.avatarList,
                selected = selected && isSelectionMode,
            )

            Spacer(modifier = Modifier.width(spacing.m))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    // При крупном шрифте имени даём две строки: обрывок из 5–7 символов нечитаем.
                    maxLines = if (LocalDensity.current.fontScale >= 1.5f) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contactSubtitle(contact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isSelectionMode) {
                // Выбор теперь считывается через аватар (фон primary + галочку)
                // и рамку карточки; отдельный Checkbox убран, чтобы не дублировать признак.
            } else {
                FavoriteAndOverflowActions(
                    contact = contact,
                    isFavorite = isFavorite,
                    showFavoriteQuickAction = showFavoriteQuickAction,
                    hasQuickActions = hasQuickActions,
                    onCall = onCall,
                    onSms = onSms,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

/** Хвостовые действия строки: favorite (если влезает) и overflow-меню. */
@Composable
private fun FavoriteAndOverflowActions(
    contact: ContactDetails,
    isFavorite: Boolean,
    showFavoriteQuickAction: Boolean,
    hasQuickActions: Boolean,
    onCall: (() -> Unit)?,
    onSms: (() -> Unit)?,
    onToggleFavorite: (() -> Unit)?,
) {
    val spacing = LocalSpacing.current
    var menuOpen by remember { mutableStateOf(false) }
    if (showFavoriteQuickAction) {
        FavoriteQuickAction(
            contactId = contact.contactId,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
        )
    }
    if (hasQuickActions) {
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("quick_actions_${contact.contactId}"),
            ) {
                BrandIcon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.action_more),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(spacing.xl),
                )
            }
            QuickActionsMenu(
                contactId = contact.contactId,
                contact = contact,
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                onCall = onCall,
                onSms = onSms,
                showFavorite = showFavoriteQuickAction,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun FavoriteQuickAction(
    contactId: Long,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
) {
    val spacing = LocalSpacing.current
    if (onToggleFavorite == null) {
        if (isFavorite) {
            BrandIcon(
                imageVector = BrandIcons.Favorite,
                contentDescription = null,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(spacing.xl),
            )
        }
        return
    }
    IconButton(
        onClick = onToggleFavorite,
        modifier =
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("quick_favorite_$contactId"),
    ) {
        BrandIcon(
            imageVector =
                if (isFavorite) BrandIcons.Favorite else BrandIcons.FavoriteBorder,
            contentDescription =
                stringResource(
                    if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                ),
            // Активное избранное — акцентное и заполненное, неактивное — нейтральное;
            // кнопка активна в обоих состояниях, disabled-тинт здесь ошибочен.
            color =
                if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(spacing.xl),
        )
    }
}

/**
 * Быстрые действия строки. Поведение одинаково для меню и свайпа и описано здесь:
 * звонок всегда подтверждается диалогом, СМС запускается сразу.
 */
@Composable
private fun QuickActionsMenu(
    contactId: Long,
    contact: ContactDetails,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCall: (() -> Unit)?,
    onSms: (() -> Unit)?,
    showFavorite: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
) {
    val ctx = LocalContext.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        if (onCall != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_call)) },
                onClick = {
                    onDismiss()
                    onCall()
                },
                leadingIcon = {
                    BrandIcon(
                        imageVector = BrandIcons.Call,
                        contentDescription = null,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                modifier = Modifier.testTag("quick_call_$contactId"),
            )
        }
        if (onSms != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_sms)) },
                onClick = {
                    onDismiss()
                    onSms()
                },
                leadingIcon = {
                    BrandIcon(
                        imageVector = BrandIcons.Message,
                        contentDescription = null,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                },
                modifier = Modifier.testTag("quick_sms_$contactId"),
            )
        }
        if (!showFavorite && onToggleFavorite != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
                        ),
                    )
                },
                onClick = {
                    onDismiss()
                    onToggleFavorite()
                },
                modifier = Modifier.testTag("quick_favorite_menu_$contactId"),
            )
        }
        contact.primaryEmail?.let { email ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_email)) },
                onClick = {
                    onDismiss()
                    openEmail(ctx, email.address)
                },
                modifier = Modifier.testTag("quick_email_$contactId"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy_email)) },
                onClick = {
                    onDismiss()
                    copyToClipboard(ctx, email.address)
                },
                modifier = Modifier.testTag("quick_copy_email_$contactId"),
            )
        }
        contact.primaryPhone?.let { phone ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy_phone)) },
                onClick = {
                    onDismiss()
                    copyToClipboard(ctx, phone.number)
                },
                modifier = Modifier.testTag("quick_copy_phone_$contactId"),
            )
        }
    }
}

@Composable
private fun SwipeActionBackground(
    direction: SwipeToDismissBoxValue,
    modifier: Modifier = Modifier,
) {
    if (direction == SwipeToDismissBoxValue.Settled) return
    val spacing = LocalSpacing.current
    val isCall = direction == SwipeToDismissBoxValue.EndToStart
    val color = if (isCall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Row(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .background(color.copy(alpha = 0.16f))
                .padding(horizontal = spacing.xl),
        horizontalArrangement = if (isCall) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandIcon(
            imageVector = if (isCall) BrandIcons.Call else BrandIcons.Message,
            contentDescription =
                stringResource(
                    if (isCall) R.string.action_call else R.string.action_send_sms,
                ),
            color = color,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun contactSubtitle(contact: ContactDetails): String =
    if (contact.phones.isEmpty()) {
        contact.primaryEmail
            ?.address
            .orEmpty()
    } else {
        val primary = contact.primaryPhone?.number.orEmpty()
        if (contact.phones.size <= 1) {
            primary
        } else {
            "$primary · " +
                pluralStringResource(
                    R.plurals.phone_count_extra,
                    contact.phones.size - 1,
                    contact.phones.size - 1,
                )
        }
    }

/** Постоянная ширина хрома строки: поля карточки, аватар, overflow-кнопка и скроллер. */
internal const val ROW_FIXED_WIDTH_DP = 204

/** Минимальная читаемая ширина колонки имени. */
internal const val MIN_NAME_WIDTH_DP = 136

/**
 * Виден ли favorite как быстрое действие: колонка имени должна остаться
 * читаемой с учётом масштаба шрифта, иначе избранное уходит в overflow.
 */
internal fun favoriteQuickActionVisible(
    screenWidthDp: Int,
    fontScale: Float,
): Boolean = screenWidthDp - ROW_FIXED_WIDTH_DP >= MIN_NAME_WIDTH_DP * fontScale
