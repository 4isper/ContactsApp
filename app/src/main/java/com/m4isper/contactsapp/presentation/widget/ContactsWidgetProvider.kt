package com.m4isper.contactsapp.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.m4isper.contactsapp.ContactsApp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.normalizeContactText
import com.m4isper.contactsapp.presentation.settings.withAppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Виджет на главном экране: показывает избранные контакты и открывает карточку
 * по нажатию. Данные читаются из репозиториев, поэтому виджет работает без запуска
 * интерфейса приложения. Реализован на Jetpack Glance по каноническому layout'у
 * «action list»: заголовок, скроллящийся список строк и пустое состояние.
 */
class ContactsWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ContactsGlanceWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val pending = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            scope.launch {
                try {
                    glanceAppWidget.updateAll(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        /** Виджет просит обновить данные: например, после изменения избранного. */
        const val ACTION_REFRESH = "com.m4isper.contactsapp.action.REFRESH_WIDGET"

        /** Обновить все экземпляры виджета: вызывается из приложения. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, ContactsWidgetProvider::class.java)
            if (manager.getAppWidgetIds(component).isEmpty()) return
            // Небольшая пауза: избранное должно успеть сохраниться до перерисовки.
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    context.sendBroadcast(
                        Intent(context, ContactsWidgetProvider::class.java).setAction(ACTION_REFRESH),
                    )
                },
                REFRESH_DELAY_MILLIS,
            )
        }

        /** Пауза перед перерисовкой виджета: ждём записи избранного. */
        private const val REFRESH_DELAY_MILLIS = 500L
    }
}

/** Содержимое виджета избранного: Glance-реализация канонического action list. */
class ContactsGlanceWidget : GlanceAppWidget() {
    // Exact: одна отрисовка под фактический размер, без набора view на каждый брекет.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val favorites = loadFavorites(context)
        // Строки резолвим до композиции: Glance не перестраивает composition
        // при смене конфигурации, поэтому LocalContext внутри был бы stale.
        // Язык — приложения, а не системы: приложение умеет переключать его само.
        val localized = context.withAppLanguage()
        val title = localized.getString(R.string.widget_title)
        val emptyText = localized.getString(R.string.widget_empty)
        provideContent {
            GlanceTheme {
                FavoritesWidgetContent(favorites, title, emptyText)
            }
        }
    }

    private suspend fun loadFavorites(context: Context): List<ContactDetails> {
        val graph = (context.applicationContext as? ContactsApp)?.appGraph ?: return emptyList()
        val ids = graph.observeFavorites().first()
        return ids
            .mapNotNull { id -> graph.getContactDetails(id) }
            .sortedBy { normalizeContactText(it.name) }
    }
}

@Composable
private fun FavoritesWidgetContent(
    favorites: List<ContactDetails>,
    title: String,
    emptyText: String,
) {
    Scaffold(
        backgroundColor = GlanceTheme.colors.widgetBackground,
        titleBar = {
            TitleBar(
                startIcon = androidx.glance.ImageProvider(R.drawable.ic_notification),
                title = title,
            )
        },
    ) {
        if (favorites.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = emptyText,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        } else {
            // Канонический скроллящийся список: содержимое обрезается по скруглению контейнера.
            Box(modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp)) {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(favorites) { contact ->
                        FavoriteRow(contact)
                    }
                }
            }
        }
    }
}

/** Строка избранного: имя и номер/почта, нажатие открывает карточку контакта. */
@Composable
private fun FavoriteRow(contact: ContactDetails) {
    val context = LocalContext.current
    val openContact =
        Intent(
            Intent.ACTION_VIEW,
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.contactId.toString()),
        ).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    Box(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .padding(12.dp)
                .clickable(actionStartActivity(openContact)),
    ) {
        Column {
            Text(
                text = contact.name,
                style =
                    TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
                maxLines = 1,
            )
            val supporting =
                contact.primaryPhone?.number
                    ?: contact.primaryEmail?.address
            if (supporting != null) {
                Text(
                    text = supporting,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                    maxLines = 1,
                )
            }
        }
    }
}
