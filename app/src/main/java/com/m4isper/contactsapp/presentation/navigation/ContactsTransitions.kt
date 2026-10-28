package com.m4isper.contactsapp.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import com.m4isper.contactsapp.ui.theme.AppMotion

private const val EXIT_PARALLAX_DIVISOR = 3

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isSameScene(): Boolean = initialState.key == targetState.key

private fun slideForward(): ContentTransform =
    (
        slideInHorizontally(AppMotion.navigation()) { it } +
            fadeIn(AppMotion.navigation())
    ) togetherWith
        (
            slideOutHorizontally(AppMotion.navigation()) {
                -it / EXIT_PARALLAX_DIVISOR
            } +
                fadeOut(AppMotion.navigation())
        )

private fun fadeInPlace(): ContentTransform =
    fadeIn(AppMotion.navigation()) togetherWith
        fadeOut(AppMotion.navigation())

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
                slideInHorizontally(AppMotion.navigation()) {
                    -it / EXIT_PARALLAX_DIVISOR
                } +
                    fadeIn(AppMotion.navigation())
            ) togetherWith
                (
                    slideOutHorizontally(AppMotion.navigation()) { it } +
                        fadeOut(AppMotion.navigation())
                )
        }
    }

/** Предиктивный жест «назад» повторяет обычную анимацию возврата. */
internal fun contactsPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
    {
        contactsPopTransitionSpec().invoke(this)
    }
