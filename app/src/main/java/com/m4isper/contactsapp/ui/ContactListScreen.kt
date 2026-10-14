package com.m4isper.contactsapp.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m4isper.contactsapp.Contact

private fun groupContactsByFirstLetter(contacts: List<Contact>): Map<Char, List<Contact>> {
    return contacts
        .filter { it.name.isNotBlank() }
        .groupBy {
            val first = it.name.first().uppercaseChar()
            if (first.isLetter()) first else '#'
        }
        .toSortedMap()
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED

private fun startCallIntent(context: Context, phone: String) {
    val callIntent = Intent(Intent.ACTION_CALL).apply {
        data = "tel:$phone".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(callIntent)
}

private fun startDialIntent(context: Context, phone: String) {
    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
        data = "tel:$phone".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(dialIntent)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val contacts by viewModel.filteredContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var hasReadPermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.READ_CONTACTS))
    }
    var pendingCall by remember { mutableStateOf<Contact?>(null) }

    fun refreshIfGranted() {
        val granted = hasPermission(context, Manifest.permission.READ_CONTACTS)
        hasReadPermission = granted
        if (granted) {
            viewModel.loadContacts(context)
        } else {
            viewModel.clearContacts()
        }
    }

    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasReadPermission = isGranted
        if (isGranted) {
            viewModel.loadContacts(context)
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val contact = pendingCall
        pendingCall = null
        if (contact == null) return@rememberLauncherForActivityResult
        if (isGranted) {
            runCatching { startCallIntent(context, contact.phone) }
                .onFailure {
                    Toast.makeText(context, "Не удалось позвонить", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Fallback без разрешения: открываем дайлер, чтобы звонок всё равно работал.
            Toast.makeText(
                context,
                "Нет разрешения на звонки — открываю набор номера",
                Toast.LENGTH_SHORT,
            ).show()
            runCatching { startDialIntent(context, contact.phone) }
        }
    }

    fun tryCall(contact: Contact) {
        if (hasPermission(context, Manifest.permission.CALL_PHONE)) {
            runCatching { startCallIntent(context, contact.phone) }
                .onFailure {
                    Toast.makeText(context, "Не удалось позвонить", Toast.LENGTH_SHORT).show()
                }
        } else {
            pendingCall = contact
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshIfGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasReadPermission) {
        PermissionRequest(
            modifier = modifier,
            onGrantClick = {
                readPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onSettingsClick = { openAppSettings(context) },
        )
    } else {
        ContactList(
            contacts = contacts,
            isLoading = isLoading,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onContactClick = ::tryCall,
            modifier = modifier,
        )
    }
}

@Composable
private fun PermissionRequest(
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Приложению нужно разрешение на доступ к контактам",
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onGrantClick) {
                Text("Разрешить")
            }
            TextButton(onClick = onSettingsClick) {
                Text("Открыть настройки")
            }
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<Contact>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Поиск") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )

        if (isLoading && contacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        "Контакты не найдены"
                    } else {
                        "Ничего не найдено"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                )
            }
            return
        }

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
}

@Composable
fun ContactItem(contact: Contact, onClick: () -> Unit) {
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
                    text = "${contact.phone} (mobile)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
