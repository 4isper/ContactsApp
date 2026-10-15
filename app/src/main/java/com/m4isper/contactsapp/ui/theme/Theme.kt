package com.m4isper.contactsapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = TealPrimaryDark,
        onPrimary = TealOnPrimaryDark,
        primaryContainer = TealPrimaryContainerDark,
        onPrimaryContainer = TealOnPrimaryContainerDark,
        secondary = TealSecondaryDark,
        tertiary = AmberTertiaryDark,
        onTertiary = AmberOnTertiaryDark,
        tertiaryContainer = AmberTertiaryContainerDark,
        onTertiaryContainer = AmberOnTertiaryContainerDark,
        surface = SurfaceDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
        onSurface = OnSurfaceDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = TealPrimaryLight,
        onPrimary = TealOnPrimaryLight,
        primaryContainer = TealPrimaryContainerLight,
        onPrimaryContainer = TealOnPrimaryContainerLight,
        secondary = TealSecondaryLight,
        tertiary = AmberTertiaryLight,
        onTertiary = AmberOnTertiaryLight,
        tertiaryContainer = AmberTertiaryContainerLight,
        onTertiaryContainer = AmberOnTertiaryContainerLight,
        surface = SurfaceLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        onSurface = OnSurfaceLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
    )

@Composable
fun ContactsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = Typography,
            content = content,
        )
    }
}
