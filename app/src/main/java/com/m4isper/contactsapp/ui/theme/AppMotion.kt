package com.m4isper.contactsapp.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion tokens for the whole app.
 *
 * Material3 1.4.0 keeps `MotionScheme` internal, so the same spec values live here.
 * Compose scales these with the system animation setting automatically.
 */
object AppMotion {
    /** Prominent, springy motion for interactive feedback (selection, toggles). */
    fun <T> expressiveSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Calmer spring for layout changes. */
    fun <T> standardSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Non-spatial motion (color, alpha): no bounce. */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

    /** Screen-to-screen travel; short enough to keep predictive back responsive. */
    fun <T> navigation(): FiniteAnimationSpec<T> =
        tween(durationMillis = NAVIGATION_DURATION_MILLIS, easing = FastOutSlowInEasing)
}

internal const val NAVIGATION_DURATION_MILLIS = 300
