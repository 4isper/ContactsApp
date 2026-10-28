package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.account.contactAccountTitle
import com.m4isper.contactsapp.presentation.contacts.ContactsEmptyReason
import com.m4isper.contactsapp.presentation.contacts.ContactsUiState
import com.m4isper.contactsapp.presentation.contacts.contactsEmptyReason
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ErrorState
import com.m4isper.contactsapp.ui.components.ListSkeleton

@Composable
internal fun ContactListBody(
    uiState: ContactsUiState,
    onContactClick: (Long) -> Unit,
    onContactLongClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onCallContact: ((ContactDetails) -> Unit)? = null,
    onSmsContact: ((ContactDetails) -> Unit)? = null,
    onToggleFavorite: ((Long) -> Unit)? = null,
    onCreateContact: (() -> Unit)? = null,
    onImportClick: (() -> Unit)? = null,
    onClearSearch: () -> Unit = {},
    onClearFilters: () -> Unit = {},
    showFavoriteQuickAction: Boolean,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier.fillMaxSize(),
    ) {
        when (val content = uiState.content) {
            is Loadable.Loading -> ListSkeleton()
            is Loadable.Empty ->
                EmptyPlaceholder(
                    reason =
                        contactsEmptyReason(
                            searchQuery = uiState.searchQuery,
                            showFavoritesOnly = uiState.showFavoritesOnly,
                            groupName = uiState.selectedGroup?.title,
                            accountName = uiState.selectedAccount?.let { contactAccountTitle(it) },
                        ),
                    onCreateContact = onCreateContact,
                    onImportClick = onImportClick,
                    onClearSearch = onClearSearch,
                    onClearFilters = onClearFilters,
                )
            is Loadable.Error ->
                ErrorState(
                    error = content.error,
                    onRetry = onRetry,
                )
            is Loadable.Content ->
                GroupedContacts(
                    contacts = content.data,
                    showFavoriteQuickAction = showFavoriteQuickAction,
                    favorites = uiState.favorites,
                    selection = uiState.selection,
                    isSelectionMode = uiState.isSelectionMode,
                    onContactClick = onContactClick,
                    onContactLongClick = onContactLongClick,
                    sortOrder = uiState.sortOrder,
                    density = uiState.listDensity,
                    pinFavorites = uiState.pinFavorites,
                    onCall = onCallContact,
                    onSms = onSmsContact,
                    onToggleFavorite = onToggleFavorite,
                )
        }
    }
}

private data class EmptyStateContent(
    val icon: ImageVector,
    val title: String,
    val primaryLabel: String? = null,
    val onPrimaryClick: (() -> Unit)? = null,
    val secondaryLabel: String? = null,
    val onSecondaryClick: (() -> Unit)? = null,
)

@Composable
private fun EmptyPlaceholder(
    reason: ContactsEmptyReason,
    onCreateContact: (() -> Unit)? = null,
    onImportClick: (() -> Unit)? = null,
    onClearSearch: () -> Unit = {},
    onClearFilters: () -> Unit = {},
) {
    val content =
        emptyStateContent(
            reason = reason,
            onCreateContact = onCreateContact,
            onImportClick = onImportClick,
            onClearSearch = onClearSearch,
            onClearFilters = onClearFilters,
        )
    EmptyState(
        icon = content.icon,
        title = content.title,
        primaryLabel = content.primaryLabel,
        onPrimaryClick = content.onPrimaryClick,
        secondaryLabel = content.secondaryLabel,
        onSecondaryClick = content.onSecondaryClick,
        modifier = Modifier.testTag("contacts_empty_state"),
    )
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun emptyStateContent(
    reason: ContactsEmptyReason,
    onCreateContact: (() -> Unit)?,
    onImportClick: (() -> Unit)?,
    onClearSearch: () -> Unit,
    onClearFilters: () -> Unit,
): EmptyStateContent =
    when (reason) {
        ContactsEmptyReason.NoContacts ->
            EmptyStateContent(
                icon = Icons.Filled.PersonSearch,
                title = stringResource(R.string.contacts_empty),
                primaryLabel =
                    if (onCreateContact != null) {
                        stringResource(R.string.contacts_empty_cta_create)
                    } else {
                        null
                    },
                onPrimaryClick = onCreateContact,
                secondaryLabel =
                    if (onImportClick != null) {
                        stringResource(R.string.contacts_empty_cta_import)
                    } else {
                        null
                    },
                onSecondaryClick = onImportClick,
            )

        is ContactsEmptyReason.Search ->
            EmptyStateContent(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.contacts_search_empty, reason.query),
                primaryLabel = stringResource(R.string.action_clear_search),
                onPrimaryClick = onClearSearch,
                secondaryLabel =
                    if (reason.hasOtherFilters) {
                        stringResource(R.string.action_clear_filters)
                    } else {
                        null
                    },
                onSecondaryClick = if (reason.hasOtherFilters) onClearFilters else null,
            )

        ContactsEmptyReason.Favorites ->
            EmptyStateContent(
                icon = Icons.Filled.FavoriteBorder,
                title = stringResource(R.string.favorites_empty),
                primaryLabel = stringResource(R.string.contacts_empty_show_all),
                onPrimaryClick = onClearFilters,
            )

        is ContactsEmptyReason.Group ->
            EmptyStateContent(
                icon = Icons.AutoMirrored.Filled.Label,
                title = stringResource(R.string.contacts_group_empty, reason.groupName),
                primaryLabel = stringResource(R.string.contacts_empty_show_all),
                onPrimaryClick = onClearFilters,
            )

        is ContactsEmptyReason.Account ->
            EmptyStateContent(
                icon = Icons.Filled.AccountCircle,
                title = stringResource(R.string.contacts_account_empty, reason.accountName),
                primaryLabel = stringResource(R.string.contacts_empty_show_all),
                onPrimaryClick = onClearFilters,
            )

        ContactsEmptyReason.CombinedFilters ->
            EmptyStateContent(
                icon = Icons.Filled.FilterList,
                title = stringResource(R.string.contacts_filters_empty),
                primaryLabel = stringResource(R.string.action_clear_filters),
                onPrimaryClick = onClearFilters,
            )
    }
