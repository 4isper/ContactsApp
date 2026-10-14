package com.m4isper.contactsapp.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.Contact

internal fun groupContactsByFirstLetter(contacts: List<Contact>): Map<Char, List<Contact>> {
    return contacts
        .filter { it.name.isNotBlank() }
        .groupBy {
            val first = it.name.first().uppercaseChar()
            if (first.isLetter()) first else '#'
        }
        .toSortedMap()
}

@Composable
internal fun ContactList(
    uiState: ContactsUiState,
    onSearchQueryChange: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text(stringResource(R.string.search_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )

        when (val content = uiState.content) {
            is ContactsContent.Loading -> LoadingState()
            is ContactsContent.Empty -> EmptyState(isSearching = content.isSearching)
            is ContactsContent.Error -> ErrorState(
                message = content.message,
                onRetry = onRetry,
            )
            is ContactsContent.Contacts -> GroupedContacts(
                contacts = content.items,
                onContactClick = onContactClick,
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isSearching) {
                stringResource(R.string.contacts_nothing_found)
            } else {
                stringResource(R.string.contacts_empty)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
        )
    }
}

@Composable
private fun ErrorState(message: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = message ?: stringResource(R.string.contacts_load_error),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun GroupedContacts(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
) {
    val grouped = groupContactsByFirstLetter(contacts)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (letter, group) ->
            stickyHeader {
                Text(
                    text = letter.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
            items(
                items = group,
                key = { "${it.id}_${it.phone}" },
            ) { contact ->
                ContactItem(contact = contact) { onContactClick(contact) }
            }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (contact.phone.isNotBlank()) {
                Text(
                    text = stringResource(R.string.phone_label_mobile, contact.phone),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
