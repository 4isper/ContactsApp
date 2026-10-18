package com.m4isper.contactsapp.presentation.details

import androidx.compose.ui.graphics.vector.ImageVector

/** Кнопка строки: иконка, подпись для accessibility и действие. */
internal data class RowAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)
