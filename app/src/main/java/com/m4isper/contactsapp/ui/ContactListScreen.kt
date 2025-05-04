package com.m4isper.contactsapp.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.m4isper.contactsapp.Contact
import com.m4isper.contactsapp.getContacts
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.provider.Settings
import androidx.compose.ui.text.style.TextAlign


private fun groupContactsByFirstLetter(contacts: List<Contact>): Map<Char, List<Contact>> {
    return contacts
        .filter { it.name.isNotBlank() }
        .sortedBy { it.name }
        .groupBy { it.name.first().uppercaseChar() }
}

@Composable
fun ContactsScreen(
    context: Context,
    modifier: Modifier = Modifier
) {
    val contacts = remember { mutableStateOf<List<Contact>>(emptyList()) }
    val hasPermission = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission.value = isGranted
        if (isGranted) {
            contacts.value = getContacts(context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED

                hasPermission.value = granted

                if (granted) {
                    contacts.value = getContacts(context)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasPermission.value) {
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            context.findActivity(),
            android.Manifest.permission.READ_CONTACTS
        )

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Приложению нужно разрешение на доступ к контактам",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (shouldShowRationale) {
                    Button(onClick = {
                        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }) {
                        Text("Разрешить")
                    }
                } else {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Открыть настройки")
                    }
                }
            }
        }
    } else {
        ContactList(
            contacts = contacts.value,
            context = context,
            modifier = modifier
        )
    }
}

fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    throw IllegalStateException("Context is not an Activity")
}


@Composable
private fun ContactList(
    contacts: List<Contact>,
    context: Context,
    modifier: Modifier = Modifier
) {
    if (contacts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Контакты не найдены",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
        return
    }

    val grouped = groupContactsByFirstLetter(contacts)

    LazyColumn(modifier = modifier.fillMaxSize()) {
        grouped.forEach { (letter, group) ->
            stickyHeader {
                Text(
                    text = letter.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            items(group) { contact ->
                ContactItem(contact = contact) {
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = "tel:${contact.phone}".toUri()
                    }

                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        context.startActivity(callIntent)
                    } else {
                        Toast.makeText(
                            context,
                            "Нет разрешения на звонки. Включите в настройках",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.toString() ?: "?",
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (contact.phone.isNotBlank()) {
                Text(
                    text = "${contact.phone} (mobile)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}