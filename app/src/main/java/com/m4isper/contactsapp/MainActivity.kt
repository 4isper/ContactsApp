package com.m4isper.contactsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.presentation.navigation.ContactsNavHost
import com.m4isper.contactsapp.presentation.settings.ThemeViewModel
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vmFactory = (application as ContactsApp).appGraph.metroViewModelFactory

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides vmFactory,
            ) {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    val themeViewModel: ThemeViewModel = metroViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    ContactsAppTheme(darkTheme = themeMode.isDark()) {
        ContactsNavHost(
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ThemeMode.isDark(): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
