package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.RefreshFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.logw
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

private const val FAVORITES_TAG = "ContactsFavorites"

/**
 * Избранное: звезды из провайдера и их переключение.
 * Список избранного приходит потоком, экран собирает его в общее состояние.
 */
@Inject
internal class ContactsFavoriteState(
    observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val refreshFavorites: RefreshFavoritesUseCase,
) {
    /** Избранные контакты из провайдера. */
    val favorites: Flow<Set<Long>> = observeFavorites()

    /** Быстрое избранное из строки списка. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: неудачная правка показывается тостом.
    suspend fun onToggleFavorite(contactId: Long) {
        try {
            toggleFavorite(contactId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(FAVORITES_TAG, "toggle favorite failed", e)
        }
    }

    /**
     * Пакетное избранное: если выбраны только избранные — снимаем отметку,
     * иначе добавляем все ещё не избранные контакты.
     */
    @Suppress("TooGenericExceptionCaught") // Осознанно: неудачная правка показывается тостом.
    suspend fun onToggleSelectedFavorite(
        selected: Set<Long>,
        favorites: Set<Long>,
    ) {
        if (selected.isEmpty()) return
        val allFavorite = selected.all { it in favorites }
        val toRemove = if (allFavorite) selected else emptySet()
        val toAdd = if (allFavorite) emptySet() else selected - favorites
        try {
            toRemove.forEach { removeFavorite(it) }
            toAdd.forEach { toggleFavorite(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(FAVORITES_TAG, "batch favorite update failed", e)
        }
    }

    /** Избранное читается из провайдера: перечитываем его вместе с контактами. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой чтения избранного не мешает списку.
    suspend fun refresh() {
        try {
            refreshFavorites()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(FAVORITES_TAG, "refresh favorites failed", e)
        }
    }
}
