package com.m4isper.contactsapp.ui.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/**
 * Ширина окна в терминах Material 3. Экраны выбирают раскладку по классу,
 * а не по `screenWidthDp`: один источник истины для всех порогов.
 */
internal enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberWindowWidthClass(): WindowWidthClass =
    windowWidthClassOf(currentWindowAdaptiveInfoV2().windowSizeClass)

/** Пороги проверяются от широкого к узкому: так требует `isWidthAtLeastBreakpoint`. */
internal fun windowWidthClassOf(sizeClass: WindowSizeClass): WindowWidthClass =
    when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
            WindowWidthClass.EXPANDED
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            WindowWidthClass.MEDIUM
        else -> WindowWidthClass.COMPACT
    }
