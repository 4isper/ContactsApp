package com.m4isper.contactsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.m4isper.contactsapp.presentation.navigation.ContactsNavHost
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = (application as ContactsApp).appGraph.metroViewModelFactory

        enableEdgeToEdge()
        setContent {
            ContactsAppTheme {
                CompositionLocalProvider(
                    LocalMetroViewModelFactory provides vmFactory,
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ContactsNavHost(
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
