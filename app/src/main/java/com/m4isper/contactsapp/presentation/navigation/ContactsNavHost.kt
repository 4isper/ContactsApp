package com.m4isper.contactsapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.m4isper.contactsapp.presentation.contacts.ContactsScreen
import com.m4isper.contactsapp.presentation.details.ContactDetailsScreen
import kotlinx.serialization.Serializable

@Serializable
data object ContactsListKey : NavKey

@Serializable
data class ContactDetailsKey(
    val contactId: Long,
) : NavKey

@Composable
fun ContactsNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(ContactsListKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<ContactsListKey> {
                    ContactsScreen(
                        onContactClick = { contactId ->
                            backStack.add(ContactDetailsKey(contactId))
                        },
                    )
                }
                entry<ContactDetailsKey> { key ->
                    ContactDetailsScreen(
                        contactId = key.contactId,
                        onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                    )
                }
            },
        modifier = modifier,
    )
}
