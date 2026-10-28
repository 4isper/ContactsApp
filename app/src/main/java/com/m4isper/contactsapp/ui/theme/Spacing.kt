package com.m4isper.contactsapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val avatarList: Dp = 48.dp,
    val avatarDetails: Dp = 72.dp,
    /** Аватар в шапке карточки: крупнее строки списка, но не доминирует над именем. */
    val avatarHero: Dp = 104.dp,
    /** Отступ снизу списка, чтобы FAB не перекрывал последний элемент. */
    val fabClearance: Dp = 80.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
