package com.m4isper.contactsapp.presentation.contacts.state

import com.m4isper.contactsapp.domain.usecase.contacts.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.RefreshFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.safeCall
import dev.zacsweers.metro.Inject
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
    suspend fun onToggleFavorite(contactId: Long) {
        safeCall(FAVORITES_TAG, "toggle favorite failed") {
            toggleFavorite(contactId)
        }
    }

    /**
     * Пакетное избранное: если выбраны только избранные — снимаем отметку,
     * иначе добавляем все ещё не избранные контакты.
     */
    suspend fun onToggleSelectedFavorite(
        selected: Set<Long>,
        favorites: Set<Long>,
    ) {
        if (selected.isEmpty()) return
        val allFavorite = selected.all { it in favorites }
        val toRemove = if (allFavorite) selected else emptySet()
        val toAdd = if (allFavorite) emptySet() else selected - favorites
        safeCall(FAVORITES_TAG, "batch favorite update failed") {
            toRemove.forEach { removeFavorite(it) }
            toAdd.forEach { toggleFavorite(it) }
        }
    }

    /** Избранное читается из провайдера: перечитываем его вместе с контактами. */
    suspend fun refresh() {
        safeCall(FAVORITES_TAG, "refresh favorites failed") {
            refreshFavorites()
        }
    }
}
