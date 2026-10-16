package com.m4isper.contactsapp.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

internal const val NAV_TRANSITION_DURATION_MILLIS = 300

private const val EXIT_PARALLAX_DIVISOR = 3

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isTwoPane(): Boolean = targetState.entries.size > 1

/**
 * Вперёд: на телефоне — системный слайд справа, на двух панелях список стоит,
 * детали мягко проявляются внутри своей панели.
 */
internal fun contactsTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        if (isTwoPane()) {
            fadeIn(tween(NAV_TRANSITION_DURATION_MILLIS)) togetherWith
                fadeOut(tween(NAV_TRANSITION_DURATION_MILLIS))
        } else {
            (
                slideInHorizontally(tween(NAV_TRANSITION_DURATION_MILLIS)) { it } +
                    fadeIn(tween(NAV_TRANSITION_DURATION_MILLIS))
            ) togetherWith
                (
                    slideOutHorizontally(tween(NAV_TRANSITION_DURATION_MILLIS)) { -it / EXIT_PARALLAX_DIVISOR } +
                        fadeOut(tween(NAV_TRANSITION_DURATION_MILLIS))
                )
        }
    }

/** Назад: зеркально вперёд. */
internal fun contactsPopTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        if (isTwoPane()) {
            fadeIn(tween(NAV_TRANSITION_DURATION_MILLIS)) togetherWith
                fadeOut(tween(NAV_TRANSITION_DURATION_MILLIS))
        } else {
            (
                slideInHorizontally(tween(NAV_TRANSITION_DURATION_MILLIS)) { -it / EXIT_PARALLAX_DIVISOR } +
                    fadeIn(tween(NAV_TRANSITION_DURATION_MILLIS))
            ) togetherWith
                (
                    slideOutHorizontally(tween(NAV_TRANSITION_DURATION_MILLIS)) { it } +
                        fadeOut(tween(NAV_TRANSITION_DURATION_MILLIS))
                )
        }
    }

/** Предиктивный жест «назад» повторяет обычную анимацию возврата. */
internal fun contactsPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
    {
        contactsPopTransitionSpec().invoke(this)
    }
