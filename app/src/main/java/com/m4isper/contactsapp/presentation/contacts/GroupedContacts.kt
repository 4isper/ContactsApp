package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

@Composable
internal fun GroupedContacts(
    contacts: List<ContactDetails>,
    favorites: Set<Long>,
    selection: Set<Long>,
    isSelectionMode: Boolean,
    onContactClick: (Long) -> Unit,
    onContactLongClick: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(
                                        horizontal = spacing.m,
                                        vertical = spacing.s,
                                    ),
                        )
                    }
                    items(
                        items = group,
                        key = { it.contactId },
                    ) { contact ->
                        ContactCard(
                            contact = contact,
                            isFavorite = contact.contactId in favorites,
                            selected = contact.contactId in selection,
                            isSelectionMode = isSelectionMode,
                            onClick = { onContactClick(contact.contactId) },
                            onLongClick = { onContactLongClick(contact.contactId) },
                        )
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
                            ).padding(spacing.xl),
                )
            }
        }
    }
}
