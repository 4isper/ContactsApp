package com.m4isper.contactsapp

import android.content.Intent
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
import com.m4isper.contactsapp.presentation.intent.StartAction
import com.m4isper.contactsapp.presentation.intent.startActionOf
import com.m4isper.contactsapp.presentation.navigation.ContactsNavHost
import com.m4isper.contactsapp.presentation.settings.ThemeViewModel
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/** Запуск приложения из системы: действие и его порядковый номер. */
data class StartRequest(
    val id: Long,
    val action: StartAction,
)

class MainActivity : ComponentActivity() {
    private var requestCounter = 0L
    private val startRequests = MutableStateFlow<StartRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishStartAction(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        publishStartAction(intent)
    }

    /** Новое действие от системы: показываем его экрану даже если оно повторяет прежнее. */
    private fun publishStartAction(intent: Intent?) {
        requestCounter += 1
        startRequests.value = StartRequest(requestCounter, startActionOf(intent, contentResolver))
    }

    @Composable
    private fun AppContent() {
        val themeViewModel: ThemeViewModel = metroViewModel()
        val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
        val startRequest by startRequests.collectAsStateWithLifecycle()

        ContactsAppTheme(darkTheme = themeMode.isDark()) {
            ContactsNavHost(
                modifier = Modifier.fillMaxSize(),
                startRequest = startRequest,
            )
        }
    }
}

@Composable
private fun ThemeMode.isDark(): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
