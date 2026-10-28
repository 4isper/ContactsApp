package com.m4isper.contactsapp.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Читаемая колонка: на широком окне контент не растягивается шире
 * [maxWidth] и центрируется по горизонтали, на узком — занимает всё место.
 */
@Composable
internal fun ReadingColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = LocalSpacing.current.contentMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            content = content,
        )
    }
}
