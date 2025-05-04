package com.m4isper.contactsapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.m4isper.contactsapp.ui.ContactsScreen
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE),
            1
        )

        enableEdgeToEdge()
        setContent {
            ContactsAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContactsScreen(
                        context = this,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
