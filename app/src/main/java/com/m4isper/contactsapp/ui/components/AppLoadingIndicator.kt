package com.m4isper.contactsapp.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** Themed indeterminate progress: one place for color, thickness, and track. */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = 4.dp,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
    )
}
