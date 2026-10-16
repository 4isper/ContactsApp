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

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isSameScene(): Boolean = initialState.key == targetState.key

private fun slideForward(): ContentTransform =
    (
        slideInHorizontally(tween(NAV_TRANSITION_DURATION_MILLIS)) { it } +
            fadeIn(tween(NAV_TRANSITION_DURATION_MILLIS))
    ) togetherWith
        (
            slideOutHorizontally(tween(NAV_TRANSITION_DURATION_MILLIS)) {
                -it / EXIT_PARALLAX_DIVISOR
            } +
                fadeOut(tween(NAV_TRANSITION_DURATION_MILLIS))
        )

private fun fadeInPlace(): ContentTransform =
    fadeIn(tween(NAV_TRANSITION_DURATION_MILLIS)) togetherWith
        fadeOut(tween(NAV_TRANSITION_DURATION_MILLIS))

/**
 * Вперёд: смена сцены (в т.ч. редактор на любом экране) — системный слайд справа,
 * смена детали внутри той же двухпанельной сцены — мягкий фейд на месте.
 */
internal fun contactsTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        if (isSameScene()) fadeInPlace() else slideForward()
    }

/** Назад и предиктивный жест — зеркально вперёд. */
internal fun contactsPopTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        if (isSameScene()) {
            fadeInPlace()
        } else {
            (
                slideInHorizontally(tween(NAV_TRANSITION_DURATION_MILLIS)) {
                    -it / EXIT_PARALLAX_DIVISOR
                } +
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
