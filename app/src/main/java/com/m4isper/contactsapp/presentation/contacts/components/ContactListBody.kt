package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.contacts.ContactsUiState
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
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    isSearching = uiState.isSearching,
                    onCreateContact = onCreateContact,
                    onImportClick = onImportClick,
                )
            is Loadable.Error ->
                ErrorState(
                    error = content.error,
                    onRetry = onRetry,
                )
            is Loadable.Content ->
                GroupedContacts(
                    contacts = content.data,
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

@Suppress("CyclomaticComplexMethod")
@Composable
private fun EmptyPlaceholder(
    showFavoritesOnly: Boolean,
    isSearching: Boolean,
    onCreateContact: (() -> Unit)? = null,
    onImportClick: (() -> Unit)? = null,
) {
    val isCompletelyEmpty = !showFavoritesOnly && !isSearching
    EmptyState(
        icon =
            when {
                showFavoritesOnly && !isSearching -> Icons.Filled.FavoriteBorder
                isSearching -> Icons.Filled.Search
                else -> Icons.Filled.PersonSearch
            },
        title =
            when {
                showFavoritesOnly && !isSearching ->
                    stringResource(R.string.favorites_empty)
                isSearching ->
                    stringResource(R.string.contacts_nothing_found)
                else -> stringResource(R.string.contacts_empty)
            },
        primaryLabel =
            if (isCompletelyEmpty) stringResource(R.string.contacts_empty_cta_create) else null,
        onPrimaryClick =
            if (isCompletelyEmpty) onCreateContact else null,
        secondaryLabel =
            if (isCompletelyEmpty && onImportClick != null) stringResource(R.string.contacts_empty_cta_import)
            else null,
        onSecondaryClick =
            if (isCompletelyEmpty) onImportClick else null,
    )
}
