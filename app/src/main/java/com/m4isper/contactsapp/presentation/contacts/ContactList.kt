package com.m4isper.contactsapp.presentation.contacts

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.hasPermission
import com.m4isper.contactsapp.presentation.common.logw
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ErrorState
import com.m4isper.contactsapp.ui.components.ListAvatar
import com.m4isper.contactsapp.ui.components.LoadingState
import com.m4isper.contactsapp.ui.components.SearchField
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXPORT_FILE_NAME = "contacts.vcf"

private const val IMPORT_TAG = "ContactsImport"

/** MIME-типы, под которыми файловые менеджеры отдают .vcf. */
private val VCARD_IMPORT_MIME_TYPES =
    arrayOf(
        "text/vcard",
        "text/x-vcard",
        "text/directory",
        "text/plain",
        "application/octet-stream",
    )

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
    onAddClick: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onContactSelected: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onDeleteSelected: () -> Unit,
    onUndoDelete: () -> Unit,
    onDeleteWindowClosed: () -> Unit,
    onShareSelected: () -> Unit,
    onImportText: (String) -> Unit,
    onExportRequested: () -> Unit,
    onEventHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    // Битый или отозванный документ не должен ронять экран: читаем безопасно,
                    // при любой ошибке пользователь увидит общий тост о неудачном импорте.
                    val text =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver
                                    .openInputStream(uri)
                                    ?.bufferedReader()
                                    ?.use { it.readText() }
                            }.onFailure { logw(IMPORT_TAG, "import file read failed", it) }.getOrNull()
                        }
                    if (text.isNullOrBlank()) {
                        Toast.makeText(context, R.string.import_failed, Toast.LENGTH_SHORT).show()
                    } else {
                        onImportText(text)
                    }
                }
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/vcard"),
        ) { uri ->
            if (uri != null) {
                pendingExportUri = uri
                onExportRequested()
            }
        }

    val writePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                importLauncher.launch(VCARD_IMPORT_MIME_TYPES)
            } else {
                Toast.makeText(context, R.string.import_no_permission, Toast.LENGTH_SHORT).show()
            }
        }

    val event = uiState.event
    LaunchedEffect(event) {
        if (event != null) {
            val exportUri = pendingExportUri
            pendingExportUri = null
            handleContactsEvent(context, event, exportUri, onEventHandled)
        }
    }

    DeleteUndoEffect(
        snackbarHostState = snackbarHostState,
        pendingDeleteIds = uiState.pendingDeleteIds,
        deleteClosing = uiState.deleteClosing,
        onUndoDelete = onUndoDelete,
        onWindowClosed = onDeleteWindowClosed,
    )

    val visibleIds = (uiState.content as? Loadable.Content)?.data?.map { it.contactId }.orEmpty()
    val allSelected = visibleIds.isNotEmpty() && uiState.selection.containsAll(visibleIds)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("undo_snackbar"),
            )
        },
        topBar = {
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    count = uiState.selection.size,
                    allSelected = allSelected,
                    onClose = onClearSelection,
                    onSelectAll = { onSelectAll(visibleIds) },
                    onShare = onShareSelected,
                    onDelete = { showDeleteDialog = true },
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_contact_fab"),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.editor_title_new),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (!uiState.isSelectionMode) {
                FilterHeader(
                    searchQuery = uiState.searchQuery,
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    onSearchQueryChange = onSearchQueryChange,
                    onFavoritesOnlyChange = onFavoritesOnlyChange,
                    onImportClick = {
                        if (hasPermission(context, Manifest.permission.WRITE_CONTACTS)) {
                            importLauncher.launch(VCARD_IMPORT_MIME_TYPES)
                        } else {
                            writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                        }
                    },
                    onExportClick = { exportLauncher.launch(EXPORT_FILE_NAME) },
                )
            }
            ContactListBody(
                uiState = uiState,
                onContactClick = { contactId ->
                    if (uiState.isSelectionMode) {
                        onContactSelected(contactId)
                    } else {
                        onContactClick(contactId)
                    }
                },
                onContactLongClick = onContactSelected,
                onRetry = onRetry,
                onRefresh = onRefresh,
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title =
                pluralStringResource(
                    R.plurals.batch_delete_title,
                    uiState.selection.size,
                    uiState.selection.size,
                ),
            text = stringResource(R.string.editor_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeleteDialog = false
                onDeleteSelected()
            },
            onDismiss = { showDeleteDialog = false },
            danger = true,
            confirmTestTag = "confirm_batch_delete",
        )
    }
}

@Composable
private fun FilterHeader(
    searchQuery: String,
    showFavoritesOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFavoritesOnlyChange: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onFavoritesOnlyChange,
                label = { Text(stringResource(R.string.filter_favorites)) },
                leadingIcon =
                    if (showFavoritesOnly) {
                        {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .padding(horizontal = spacing.m, vertical = spacing.xs)
                        .testTag("favorites_chip"),
            )
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("more_actions_button"),
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_import)) },
                        onClick = {
                            menuOpen = false
                            onImportClick()
                        },
                        modifier = Modifier.testTag("import_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export)) },
                        onClick = {
                            menuOpen = false
                            onExportClick()
                        },
                        modifier = Modifier.testTag("export_menu_item"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactListBody(
    uiState: ContactsUiState,
    onContactClick: (Long) -> Unit,
    onContactLongClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier.fillMaxSize(),
    ) {
        when (val content = uiState.content) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Empty ->
                EmptyPlaceholder(
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    isSearching = uiState.isSearching,
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
                    selection = uiState.selection,
                    isSelectionMode = uiState.isSelectionMode,
                    onContactClick = onContactClick,
                    onContactLongClick = onContactLongClick,
                )
        }
    }
}

@Composable
private fun EmptyPlaceholder(
    showFavoritesOnly: Boolean,
    isSearching: Boolean,
) {
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
    )
}

@Composable
private fun GroupedContacts(
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
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun ContactCard(
    contact: ContactDetails,
    isFavorite: Boolean,
    selected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .clip(MaterialTheme.shapes.medium)
                .testTag("contact_row_${contact.contactId}")
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            ListAvatar(
                name = contact.name,
                photoUri = contact.photoUri,
            )

            Spacer(modifier = Modifier.width(spacing.m))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = contactSubtitle(contact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                )
            } else if (isFavorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(spacing.m + spacing.s),
                )
            }
        }
    }
}

@Composable
private fun contactSubtitle(contact: ContactDetails): String =
    if (contact.phones.isEmpty()) {
        contact.emails
            .firstOrNull()
            ?.address
            .orEmpty()
    } else {
        val primary = contact.primaryPhone
        if (contact.phoneCount <= 1) {
            primary
        } else {
            "$primary · " +
                pluralStringResource(
                    R.plurals.phone_count_extra,
                    contact.phoneCount - 1,
                    contact.phoneCount - 1,
                )
        }
    }
