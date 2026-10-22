package com.m4isper.contactsapp.presentation.navigation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.StartRequest
import com.m4isper.contactsapp.presentation.contacts.ContactsNavigationAction
import com.m4isper.contactsapp.presentation.contacts.ContactsScreen
import com.m4isper.contactsapp.presentation.details.ContactDetailsScreen
import com.m4isper.contactsapp.presentation.duplicates.DuplicatesScreen
import com.m4isper.contactsapp.presentation.editor.ContactEditorScreen
import com.m4isper.contactsapp.presentation.groups.GroupDetailScreen
import com.m4isper.contactsapp.presentation.groups.GroupsScreen
import com.m4isper.contactsapp.presentation.import.ImportPreviewScreen
import com.m4isper.contactsapp.presentation.import.ImportPreviewViewModel
import com.m4isper.contactsapp.presentation.intent.StartAction
import com.m4isper.contactsapp.presentation.recents.RecentsScreen
import com.m4isper.contactsapp.presentation.settings.SettingsScreen
import com.m4isper.contactsapp.ui.components.EmptyState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ContactsListKey : NavKey

@Serializable
data object GroupsKey : NavKey

@Serializable
data object DuplicatesKey : NavKey

@Serializable
data object RecentsKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object ImportPreviewKey : NavKey

@Serializable
data class GroupDetailKey(
    val groupId: Long,
) : NavKey

@Serializable
data class ContactDetailsKey(
    val contactId: Long,
) : NavKey

@Serializable
data class ContactEditorKey(
    val contactId: Long? = null,
    /** Номер сеанса правки: новый сеанс — новая форма, даже для того же контакта. */
    val sessionId: Long = 0L,
) : NavKey

/**
 * Открыть детали: на узком экране — push поверх списка,
 * на широком (рядом уже открыты детали) — замена текущей записи,
 * чтобы стек не рос.
 */
internal fun navigateToDetails(
    backStack: MutableList<NavKey>,
    contactId: Long,
) {
    val key = ContactDetailsKey(contactId)
    if (backStack.lastOrNull() is ContactDetailsKey) {
        backStack[backStack.lastIndex] = key
    } else {
        backStack.add(key)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ContactsNavHost(
    modifier: Modifier = Modifier,
    startRequest: StartRequest? = null,
    onRestartRequired: () -> Unit = {},
) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(ContactsListKey)
    val importViewModel: ImportPreviewViewModel = metroViewModel()
    var pickMode by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var editorSession by remember { mutableLongStateOf(0L) }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive =
        remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    // Запуск из системы: открыть контакт, создать, выбрать или принять .vcf.
    LaunchedEffect(startRequest?.id) {
        when (val action = startRequest?.action) {
            null,
            StartAction.List,
            -> Unit
            is StartAction.OpenContact -> backStack.add(ContactDetailsKey(action.contactId))
            is StartAction.EditContact -> backStack.add(ContactEditorKey(action.contactId, editorSession++))
            StartAction.NewContact -> backStack.add(ContactEditorKey(null, editorSession++))
            StartAction.PickContact -> pickMode = true
            StartAction.ShowFavorites -> showFavorites = true
            is StartAction.ImportVCard -> {
                importViewModel.prepare(action.text)
                backStack.add(ImportPreviewKey)
            }
        }
    }

    // Непрозрачная подложка: прозрачные корни панелей не должны светить фоном окна.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(listDetailStrategy),
            transitionSpec = { contactsTransitionSpec().invoke(this) },
            popTransitionSpec = { contactsPopTransitionSpec().invoke(this) },
            predictivePopTransitionSpec = { contactsPredictivePopTransitionSpec().invoke(this, it) },
            entryProvider =
                entryProvider {
                    entry<ContactsListKey>(
                        metadata =
                            ListDetailSceneStrategy.listPane(
                                detailPlaceholder = {
                                    EmptyState(
                                        icon = Icons.Filled.Contacts,
                                        title = stringResource(R.string.contact_select_prompt),
                                    )
                                },
                            ),
                    ) {
                        ContactsScreen(
                            showFavorites = showFavorites,
                            onNavigation = { action ->
                                handleContactsNavigation(
                                    action = action,
                                    backStack = backStack,
                                    pickMode = pickMode,
                                    context = context,
                                    editorSession = editorSession,
                                )
                                editorSession++
                            },
                            onImportPreviewReady = {
                                backStack.add(ImportPreviewKey)
                            },
                        )
                    }
                    entry<ImportPreviewKey> {
                        ImportPreviewScreen(
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                            onDone = dropUnlessResumed { backStack.removeLastOrNull() },
                        )
                    }
                    entry<GroupsKey> {
                        GroupsScreen(
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                            onGroupClick = { groupId -> backStack.add(GroupDetailKey(groupId)) },
                        )
                    }
                    entry<GroupDetailKey> { key ->
                        GroupDetailScreen(
                            groupId = key.groupId,
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                            onContactClick = { contactId -> navigateToDetails(backStack, contactId) },
                        )
                    }
                    entry<DuplicatesKey> {
                        DuplicatesScreen(
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                        )
                    }
                    entry<RecentsKey> {
                        RecentsScreen(
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                            onContactClick = { contactId ->
                                navigateToDetails(backStack, contactId)
                            },
                        )
                    }
                    entry<SettingsKey> {
                        SettingsScreen(
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                            onRestartRequired = onRestartRequired,
                        )
                    }
                    entry<ContactDetailsKey>(
                        metadata = ListDetailSceneStrategy.detailPane(),
                    ) { key ->
                        ContactDetailsScreen(
                            contactId = key.contactId,
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                            onEdit = { backStack.add(ContactEditorKey(key.contactId, editorSession++)) },
                        )
                    }
                    entry<ContactEditorKey> { key ->
                        ContactEditorScreen(
                            contactId = key.contactId,
                            sessionId = key.sessionId,
                            onDone = { backStack.removeLastOrNull() },
                            onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                        )
                    }
                },
        )
    }
}

/** Переходы экрана списка: контакт, добавление, группы, дубликаты, недавние и настройки. */
private fun handleContactsNavigation(
    action: ContactsNavigationAction,
    backStack: MutableList<NavKey>,
    pickMode: Boolean,
    context: Context,
    editorSession: Long,
) {
    when (action) {
        is ContactsNavigationAction.ContactClicked ->
            if (pickMode) {
                // Режим выбора: отдаём контакт запросившей программе.
                context.pickContactResult(action.contactId)
            } else {
                navigateToDetails(backStack, action.contactId)
            }
        ContactsNavigationAction.AddClicked -> backStack.add(ContactEditorKey(null, editorSession))
        ContactsNavigationAction.ManageGroupsClicked -> backStack.add(GroupsKey)
        ContactsNavigationAction.DuplicatesClicked -> backStack.add(DuplicatesKey)
        ContactsNavigationAction.RecentsClicked -> backStack.add(RecentsKey)
        ContactsNavigationAction.SettingsClicked -> backStack.add(SettingsKey)
        is ContactsNavigationAction.ProfileClicked -> navigateToDetails(backStack, action.contactId)
    }
}
