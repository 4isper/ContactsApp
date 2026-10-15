package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.presentation.common.ContactAvatar
import com.m4isper.contactsapp.presentation.common.ErrorState
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.LoadingState
import kotlinx.coroutines.launch

internal fun groupContactsByFirstLetter(contacts: List<ContactDetails>): Map<Char, List<ContactDetails>> =
    contacts
        .filter { it.name.isNotBlank() }
        .groupBy {
            val first = it.name.first().uppercaseChar()
            if (first.isLetter()) first else '#'
        }.toSortedMap()

/** Индекс первого элемента (sticky-заголовка) каждой буквы в LazyColumn. */
internal fun letterIndices(grouped: Map<Char, List<ContactDetails>>): Map<Char, Int> {
    var index = 0
    return buildMap {
        grouped.forEach { (letter, group) ->
            put(letter, index)
            index += 1 + group.size
        }
    }
}

@Composable
internal fun ContactList(
    uiState: ContactsUiState,
    onSearchQueryChange: (String) -> Unit,
    onFavoritesOnlyChange: () -> Unit,
    onContactClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text(stringResource(R.string.search_label)) },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
                    .testTag("search_field"),
        )
        FilterChip(
            selected = uiState.showFavoritesOnly,
            onClick = onFavoritesOnlyChange,
            label = { Text(stringResource(R.string.filter_favorites)) },
            leadingIcon =
                if (uiState.showFavoritesOnly) {
                    {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                        )
                    }
                } else {
                    null
                },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).testTag("favorites_chip"),
        )

        when (val content = uiState.content) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Empty ->
                EmptyState(
                    text =
                        when {
                            uiState.showFavoritesOnly && !uiState.isSearching ->
                                stringResource(R.string.favorites_empty)
                            uiState.isSearching ->
                                stringResource(R.string.contacts_nothing_found)
                            else -> stringResource(R.string.contacts_empty)
                        },
                )
            is Loadable.Error ->
                ErrorState(
                    message = content.message,
                    onRetry = onRetry,
                )
            is Loadable.Content ->
                GroupedContacts(
                    contacts = content.data,
                    favorites = uiState.favorites,
                    onContactClick = onContactClick,
                )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
        )
    }
}

@Composable
private fun GroupedContacts(
    contacts: List<ContactDetails>,
    favorites: Set<Long>,
    onContactClick: (Long) -> Unit,
) {
    val grouped = groupContactsByFirstLetter(contacts)
    val indices = remember(grouped) { letterIndices(grouped) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<Char?>(null) }

    fun jumpTo(letter: Char) {
        activeLetter = letter
        indices[letter]?.let { index ->
            scope.launch { listState.scrollToItem(index) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize(),
            ) {
                grouped.forEach { (letter, group) ->
                    stickyHeader {
                        Text(
                            text = letter.toString(),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(
                        items = group,
                        key = { it.contactId },
                    ) { contact ->
                        ContactItem(
                            contact = contact,
                            isFavorite = contact.contactId in favorites,
                        ) { onContactClick(contact.contactId) }
                    }
                }
            }
            if (grouped.size > 1) {
                FastScroller(
                    letters = grouped.keys.toList(),
                    activeLetter = activeLetter,
                    barDescription = stringResource(R.string.fast_scroller_desc),
                    onLetter = ::jumpTo,
                    onRelease = { activeLetter = null },
                )
            }
        }
        activeLetter?.let { letter ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier =
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ).padding(24.dp),
                )
            }
        }
    }
}

/** Чистый маппинг Y-координаты касания в букву индекса. */
internal fun letterIndexAt(
    y: Float,
    height: Int,
    size: Int,
): Int? {
    if (height <= 0 || size <= 0) return null
    return ((y / height) * size).toInt().coerceIn(0, size - 1)
}

@Composable
private fun FastScroller(
    letters: List<Char>,
    activeLetter: Char?,
    barDescription: String,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun letterAt(
        y: Float,
        height: Int,
    ): Char? = letterIndexAt(y, height, letters.size)?.let { letters[it] }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            modifier
                .fillMaxHeight()
                .width(28.dp)
                .semantics {
                    contentDescription = barDescription
                    stateDescription = activeLetter?.toString() ?: ""
                }.pointerInput(letters) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        letterAt(down.position.y, size.height)?.let(onLetter)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            letterAt(change.position.y, size.height)?.let(onLetter)
                            change.consume()
                        } while (true)
                        onRelease()
                    }
                },
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = if (letter == activeLetter) FontWeight.Bold else FontWeight.Normal,
                color =
                    if (letter == activeLetter) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Gray
                    },
            )
        }
    }
}

@Composable
private fun ContactItem(
    contact: ContactDetails,
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp)
                .testTag("contact_row_${contact.contactId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(
            name = contact.name,
            photoUri = contact.photoUri,
            size = 48.dp,
            textStyle = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = contactSubtitle(contact),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun contactSubtitle(contact: ContactDetails): String {
    val primary = contact.primaryPhone
    if (contact.phoneCount <= 1) return primary
    return "$primary · " +
        pluralStringResource(
            R.plurals.phone_count_extra,
            contact.phoneCount - 1,
            contact.phoneCount - 1,
        )
}
