package com.m4isper.contactsapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.presentation.intent.StartAction
import com.m4isper.contactsapp.presentation.intent.startActionOf
import com.m4isper.contactsapp.presentation.navigation.ContactsNavHost
import com.m4isper.contactsapp.presentation.settings.AppLockScreen
import com.m4isper.contactsapp.presentation.settings.AppLockViewModel
import com.m4isper.contactsapp.presentation.settings.AppSettingsViewModel
import com.m4isper.contactsapp.presentation.settings.SharedPrefsAppLanguageStore
import com.m4isper.contactsapp.ui.components.LocalShowPhotos
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/** Запуск приложения из системы: действие и его порядковый номер. */
data class StartRequest(
    val id: Long,
    val action: StartAction,
)

class MainActivity : ComponentActivity() {
    private var requestCounter = 0L
    private val startRequests = MutableStateFlow<StartRequest?>(null)

    override fun attachBaseContext(newBase: Context) {
        // Язык приложения хранится в обычных настройках: он нужен до запуска Activity.
        val language = SharedPrefsAppLanguageStore(newBase).language
        super.attachBaseContext(language.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishStartAction(intent)

        val vmFactory = (application as ContactsApp).appGraph.metroViewModelFactory

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides vmFactory,
            ) {
                AppContent(startRequests, onRestartRequired = { recreate() })
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
}

@Composable
private fun AppContent(
    startRequests: Flow<StartRequest?>,
    onRestartRequired: () -> Unit,
) {
    val settingsViewModel: AppSettingsViewModel = metroViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val lockViewModel: AppLockViewModel = metroViewModel()
    val unlocked by lockViewModel.isUnlocked.collectAsStateWithLifecycle()
    val lockRemaining by lockViewModel.lockRemaining.collectAsStateWithLifecycle()
    val startRequest by startRequests.collectAsStateWithLifecycle(initialValue = null)

    // Уход в фон снова включает защиту приложения.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        lockViewModel.lock()
    }
    SecureWindowEffect(settings.secureWindow)

    ContactsAppTheme(
        darkTheme = settings.themeMode.isDark(),
        dynamicColor = settings.dynamicColor,
    ) {
        CompositionLocalProvider(LocalShowPhotos provides settings.showPhotos) {
            if (settings.appLockEnabled && !unlocked) {
                AppLockScreen(
                    onUnlock = { pin -> lockViewModel.unlock(pin, settings.appLockPinHash) },
                    lockedFor = lockRemaining,
                )
            } else {
                ContactsNavHost(
                    modifier = Modifier.fillMaxSize(),
                    startRequest = startRequest,
                    // Язык приложения применяется при создании Activity: пересоздаём окно.
                    onRestartRequired = onRestartRequired,
                )
            }
        }
    }
}

/** Скрытие содержимого окна в списке недавних задач. */
@Composable
private fun SecureWindowEffect(enabled: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(enabled) {
        val activity = context as? Activity ?: return@LaunchedEffect
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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

/** Оборачивает контекст выбранным языком приложения. */
private fun String.wrap(base: Context): Context {
    if (isBlank()) return base
    val locale = Locale.forLanguageTag(this)
    Locale.setDefault(locale)
    val configuration = Configuration(base.resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return base.createConfigurationContext(configuration)
}
