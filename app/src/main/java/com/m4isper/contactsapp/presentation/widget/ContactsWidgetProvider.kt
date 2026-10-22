package com.m4isper.contactsapp.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.View
import android.widget.RemoteViews
import com.m4isper.contactsapp.ContactsApp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.normalizeContactText
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Виджет на главном экране: показывает избранные контакты и открывает карточку
 * по нажатию. Данные читаются из репозиториев, поэтому виджет работает без запуска
 * интерфейса приложения.
 */
class ContactsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val favorites = loadFavorites(appContext)
                appWidgetIds.forEach { id ->
                    updateWidget(appContext, appWidgetManager, id, favorites)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun loadFavorites(context: Context): List<ContactDetails> {
        val graph = (context.applicationContext as? ContactsApp)?.appGraph ?: return emptyList()
        val ids = ObserveFavoritesUseCase(graph.favoritesRepository)().first()
        return ids
            .mapNotNull { id -> graph.contactsRepository.getContactDetails(id) }
            .sortedBy { normalizeContactText(it.name) }
    }

    private fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        favorites: List<ContactDetails>,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_contacts)
        val rows = listOf(R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3, R.id.widget_row_4, R.id.widget_row_5)
        rows.forEachIndexed { index, rowId ->
            val contact = favorites.getOrNull(index)
            if (contact == null) {
                views.setViewVisibility(rowId, View.GONE)
            } else {
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(rowId, contact.name)
                views.setOnClickPendingIntent(rowId, openContactIntent(context, contact))
            }
        }
        if (favorites.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE)
        }
        manager.updateAppWidget(widgetId, views)
    }

    /** Нажатие на строку виджета открывает карточку контакта в приложении. */
    private fun openContactIntent(
        context: Context,
        contact: ContactDetails,
    ): PendingIntent {
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.contactId.toString()),
            ).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return PendingIntent.getActivity(
            context,
            contact.contactId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids =
                manager.getAppWidgetIds(
                    android.content.ComponentName(context, ContactsWidgetProvider::class.java),
                )
            onUpdate(context, manager, ids)
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
