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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = TealPrimaryDark,
        onPrimary = TealOnPrimaryDark,
        primaryContainer = TealPrimaryContainerDark,
        onPrimaryContainer = TealOnPrimaryContainerDark,
        secondary = TealSecondaryDark,
        onSecondary = TealOnPrimaryContainerDark,
        secondaryContainer = TealSecondaryContainerDark,
        onSecondaryContainer = TealOnSecondaryContainerDark,
        tertiary = AmberTertiaryDark,
        onTertiary = AmberOnTertiaryDark,
        tertiaryContainer = AmberTertiaryContainerDark,
        onTertiaryContainer = AmberOnTertiaryContainerDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        background = SurfaceDark,
        onBackground = OnSurfaceDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceContainerHighestDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        surfaceTint = TealPrimaryDark,
        inverseSurface = InverseSurfaceDark,
        inverseOnSurface = InverseOnSurfaceDark,
        inversePrimary = InversePrimaryDark,
        surfaceDim = SurfaceDimDark,
        surfaceBright = SurfaceBrightDark,
        surfaceContainerLowest = SurfaceContainerLowestDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        surfaceContainerHighest = SurfaceContainerHighestDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        scrim = ScrimDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = TealPrimaryLight,
        onPrimary = TealOnPrimaryLight,
        primaryContainer = TealPrimaryContainerLight,
        onPrimaryContainer = TealOnPrimaryContainerLight,
        secondary = TealSecondaryLight,
        onSecondary = TealOnPrimaryContainerLight,
        secondaryContainer = TealSecondaryContainerLight,
        onSecondaryContainer = TealOnSecondaryContainerLight,
        tertiary = AmberTertiaryLight,
        onTertiary = AmberOnTertiaryLight,
        tertiaryContainer = AmberTertiaryContainerLight,
        onTertiaryContainer = AmberOnTertiaryContainerLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        background = SurfaceLight,
        onBackground = OnSurfaceLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceContainerHighestLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        surfaceTint = TealPrimaryLight,
        inverseSurface = InverseSurfaceLight,
        inverseOnSurface = InverseOnSurfaceLight,
        inversePrimary = InversePrimaryLight,
        surfaceDim = SurfaceDimLight,
        surfaceBright = SurfaceBrightLight,
        surfaceContainerLowest = SurfaceContainerLowestLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        surfaceContainerHighest = SurfaceContainerHighestLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
        scrim = ScrimLight,
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
        LocalBrandTypography provides BrandTypography(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = Typography,
            content = content,
        )
    }
}
