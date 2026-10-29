package com.m4isper.contactsapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.presentation.intent.StartAction
import com.m4isper.contactsapp.presentation.intent.startActionOf
import com.m4isper.contactsapp.presentation.navigation.ContactsNavHost
import com.m4isper.contactsapp.presentation.settings.AppLockScreen
import com.m4isper.contactsapp.presentation.settings.AppLockViewModel
import com.m4isper.contactsapp.presentation.settings.AppSettingsViewModel
import com.m4isper.contactsapp.presentation.settings.LocaleManagerSystemAppLanguage
import com.m4isper.contactsapp.presentation.settings.SharedPrefsAppLanguageStore
import com.m4isper.contactsapp.presentation.settings.withAppLanguage
import com.m4isper.contactsapp.ui.components.LocalShowPhotos
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Запуск приложения из системы: действие и его порядковый номер. */
data class StartRequest(
    val id: Long,
    val action: StartAction,
)

class MainActivity : FragmentActivity() {
    private var requestCounter = 0L
    private val startRequests = MutableStateFlow<StartRequest?>(null)

    override fun attachBaseContext(newBase: Context) {
        // Язык может меняться и с системной страницы «Язык приложения» (Android 13+):
        // хранилище синхронизируем до выбора локали, иначе fallback возьмёт устаревшее.
        syncStoredAppLanguage(newBase)
        super.attachBaseContext(newBase.withAppLanguage())
    }

    /** Держит хранилище языка равным системному выбору, чтобы fallback после «языка системы» не воскресил старый. */
    private fun syncStoredAppLanguage(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val store = SharedPrefsAppLanguageStore(context)
        val system = LocaleManagerSystemAppLanguage(context).current()
        if (system != store.language) store.language = system
    }

    /** Настройки показывают выбранный язык: после смены с системной страницы актуализируем настройку. */
    private fun syncLanguageSetting() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val graph = (applicationContext as? ContactsApp)?.appGraph ?: return
        lifecycleScope.launch {
            val system = LocaleManagerSystemAppLanguage(applicationContext).current()
            val settings = graph.observeSettings().first()
            if (settings.appLanguage != system) graph.setAppLanguage(system)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishStartAction(intent)
        syncLanguageSetting()

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
        val requestId = requestCounter
        lifecycleScope.launch {
            val action =
                withContext(Dispatchers.IO) {
                    startActionOf(intent, contentResolver)
                }
            startRequests.value = StartRequest(requestId, action)
        }
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

    // Защита включается только при реальном уходе в фон: поворот и быстрый
    // возврат из внешней активности не должны сбрасывать состояние редактора.
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    DisposableEffect(lifecycleOwner) {
        var lockJob: Job? = null
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        if (activity?.isChangingConfigurations == true) return@LifecycleEventObserver
                        lockJob?.cancel()
                        lockJob =
                            scope.launch {
                                delay(APP_LOCK_DELAY_MILLIS)
                                lockViewModel.lock()
                            }
                    }
                    Lifecycle.Event.ON_START -> {
                        lockJob?.cancel()
                        lockJob = null
                    }
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lockJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    SecureWindowEffect(settings.secureWindow)

    val darkTheme = settings.themeMode.isDark()
    // Системные бары следуют теме приложения, а не системы; пересчитывается сразу при смене темы.
    DisposableEffect(darkTheme) {
        (activity as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
        )
        onDispose { }
    }

    ContactsAppTheme(
        darkTheme = darkTheme,
        dynamicColor = settings.dynamicColor,
    ) {
        CompositionLocalProvider(LocalShowPhotos provides settings.showPhotos) {
            // До загрузки настроек защита считается включённой, но биометрию
            // не предлагаем: иначе промпт вспыхивал бы при выключенной защите.
            val lockActive = settings.appLockEnabled || !settings.settingsLoaded
            if (lockActive && !unlocked) {
                val biometricsOffered = settings.appLockEnabled && settings.settingsLoaded && settings.appLockBiometrics
                AppLockScreen(
                    onUnlock = { pin -> lockViewModel.unlock(pin, settings.appLockPinHash, settings.appLockPinLength) },
                    onBiometricUnlock = lockViewModel::unlockBiometric,
                    biometricsEnabled = biometricsOffered,
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

private const val APP_LOCK_DELAY_MILLIS = 1500L

@Composable
private fun ThemeMode.isDark(): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
