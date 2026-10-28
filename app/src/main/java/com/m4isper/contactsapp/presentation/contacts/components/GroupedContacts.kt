package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.contacts.FAVORITES_GROUP
import com.m4isper.contactsapp.domain.contacts.groupContactsWithPinnedFavorites
import com.m4isper.contactsapp.domain.contacts.letterIndices
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

@Composable
internal fun GroupedContacts(
    contacts: List<ContactDetails>,
    showFavoriteQuickAction: Boolean,
    favorites: Set<Long>,
    selection: Set<Long>,
    isSelectionMode: Boolean,
    onContactClick: (Long) -> Unit,
    onContactLongClick: (Long) -> Unit,
    sortOrder: ContactSortOrder = ContactSortOrder.BY_NAME,
    density: ListDensity = ListDensity.COMFORTABLE,
    pinFavorites: Boolean = false,
    onCall: ((ContactDetails) -> Unit)? = null,
    onSms: ((ContactDetails) -> Unit)? = null,
    onToggleFavorite: ((Long) -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val grouped =
        remember(contacts, favorites, sortOrder, pinFavorites) {
            groupContactsWithPinnedFavorites(contacts, favorites, sortOrder, pinFavorites)
        }
    val showLetterHeaders = sortOrder != ContactSortOrder.RECENTLY_ADDED
    val letters = remember(grouped) { grouped.keys.filter { it.isLetter() } }
    val indices = remember(grouped) { letterIndices(grouped) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    val scrollProgress by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val first = info.visibleItemsInfo.firstOrNull()
            if (total <= 1 || first == null) {
                0f
            } else {
                val fraction =
                    if (first.size == 0) {
                        0f
                    } else {
                        ((info.viewportStartOffset - first.offset).toFloat() / first.size)
                            .coerceIn(0f, 1f)
                    }
                ((first.index + fraction) / (total - 1)).coerceIn(0f, 1f)
            }
        }
    }

    val sectionStarts = remember(letters, indices) { letters.map { it to indices.getValue(it) } }
    val currentLetter by remember(sectionStarts) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            sectionStarts.lastOrNull { (_, start) -> start <= firstVisible }?.first
        }
    }

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
                contentPadding =
                    PaddingValues(
                        bottom = if (isSelectionMode) spacing.m else spacing.fabClearance,
                    ),
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize(),
            ) {
                grouped.forEach { (letter, group) ->
                    contactGroup(
                        letter = letter,
                        group = group,
                        showHeader = showLetterHeaders || letter == FAVORITES_GROUP,
                        favorites = favorites,
                        selection = selection,
                        isSelectionMode = isSelectionMode,
                        density = density,
                        showFavoriteQuickAction = showFavoriteQuickAction,
                        onContactClick = onContactClick,
                        onContactLongClick = onContactLongClick,
                        onCall = onCall,
                        onSms = onSms,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
            if (shouldShowFastScroller(showLetterHeaders, letters)) {
                FastScroller(
                    letters = letters,
                    activeLetter = activeLetter,
                    currentLetter = currentLetter,
                    scrollProgress = scrollProgress,
                    barDescription = stringResource(R.string.fast_scroller_desc),
                    onLetter = ::jumpTo,
                    onRelease = { activeLetter = null },
                )
            }
        }
    }
}

private fun shouldShowFastScroller(
    showLetterHeaders: Boolean,
    letters: List<Char>,
): Boolean = showLetterHeaders && letters.size > 1

private fun LazyListScope.contactGroup(
    letter: Char,
    group: List<ContactDetails>,
    showHeader: Boolean,
    favorites: Set<Long>,
    selection: Set<Long>,
    isSelectionMode: Boolean,
    density: ListDensity,
    onContactClick: (Long) -> Unit,
    onContactLongClick: (Long) -> Unit,
    showFavoriteQuickAction: Boolean,
    onCall: ((ContactDetails) -> Unit)?,
    onSms: ((ContactDetails) -> Unit)?,
    onToggleFavorite: ((Long) -> Unit)?,
) {
    if (showHeader) {
        stickyHeader { ContactGroupHeader(letter) }
    }
    itemsIndexed(
        items = group,
        key = { _, contact -> contact.contactId },
    ) { index, contact ->
        ContactCard(
            contact = contact,
            isFavorite = contact.contactId in favorites,
            selected = contact.contactId in selection,
            isSelectionMode = isSelectionMode,
            onClick = { onContactClick(contact.contactId) },
            onLongClick = { onContactLongClick(contact.contactId) },
            density = density,
            onCall = onCall?.let { action -> { action(contact) } },
            onSms = onSms?.let { action -> { action(contact) } },
            onToggleFavorite = onToggleFavorite?.let { action -> { action(contact.contactId) } },
            showFavoriteQuickAction = showFavoriteQuickAction,
            // Разделитель рисуется внутри строки: он не добавляет элемент
            // в LazyColumn и не сдвигает letterIndices.
            showDivider = index < group.lastIndex,
        )
    }
}

@Composable
private fun ContactGroupHeader(letter: Char) {
    val spacing = LocalSpacing.current
    val isFavorites = letter == FAVORITES_GROUP
    Text(
        text =
            if (isFavorites) {
                stringResource(R.string.filter_favorites)
            } else {
                letter.toString()
            },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    // Заголовок делит левую грань с аватаром строки.
                    horizontal = spacing.m,
                    vertical = spacing.s,
                ).then(
                    if (isFavorites) {
                        Modifier.testTag("favorites_section_header")
                    } else {
                        Modifier
                    },
                ),
    )
}
